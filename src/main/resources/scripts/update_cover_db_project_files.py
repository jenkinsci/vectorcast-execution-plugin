#!/usr/bin/env python3
"""Compare source-file checksums and optionally update project_files metadata.

The first database is the base/target database (R1).
The second database is the newer/source database (R2).

Workflow:
  1. Match source_files rows by normalized path.
  2. Find paths whose source_files.checksum differs.
  3. For each changed path, locate project_files rows in R2.
  4. Match each R2 project to R1 by normalized projects.path.
  5. Match the source file in R1 by normalized source_files.path.
  6. Copy only project_files.timestamp and project_files.build_md5sum.

Numeric IDs are never assumed to match between databases.

By default the script performs a dry run. Use --apply to update database1.
When --apply is used, a backup is created unless --no-backup is specified.

Exit codes:
  0 = completed successfully
  1 = differences or skipped/unmatched rows were found during comparison/dry-run
  2 = an error occurred
"""

from __future__ import annotations

import argparse
import os
import shutil
import sqlite3
import sys
from contextlib import closing
from collections import defaultdict
from dataclasses import dataclass
from pathlib import Path
from typing import DefaultDict, Iterable


@dataclass(frozen=True)
class SourceFile:
    id: int
    path: str
    checksum: int | None


@dataclass(frozen=True)
class ProjectFileMetadata:
    project_file_id: int
    project_id: int
    project_name: str | None
    project_path: str
    source_file_id: int
    timestamp: int | None
    build_md5sum: str | None


@dataclass(frozen=True)
class PlannedUpdate:
    source_path: str
    project_path: str
    project_name: str | None
    r1_project_file_id: int
    r1_source_file_id: int
    r2_source_file_id: int
    old_timestamp: int | None
    new_timestamp: int | None
    old_build_md5sum: str | None
    new_build_md5sum: str | None


def normalize_path(path: str, *, case_sensitive: bool) -> str:
    """Normalize slash style and redundant path components for comparison."""
    normalized = os.path.normpath(path.replace("\\", "/")).replace("\\", "/")
    return normalized if case_sensitive else normalized.casefold()


def quote_identifier(name: str) -> str:
    """Quote a validated SQLite identifier."""
    return '"' + name.replace('"', '""') + '"'


def connect_read_only(database: Path) -> sqlite3.Connection:
    uri = database.resolve().as_uri() + "?mode=ro"
    connection = sqlite3.connect(uri, uri=True)
    connection.row_factory = sqlite3.Row
    return connection


def validate_schema(connection: sqlite3.Connection, path_column: str) -> None:
    required_columns = {
        "source_files": {"id", path_column, "checksum"},
        "projects": {"id", "name", "path"},
        "project_files": {
            "id",
            "project_id",
            "source_file_id",
            "timestamp",
            "build_md5sum",
        },
    }

    for table, required in required_columns.items():
        columns = {
            row[1] for row in connection.execute(f"PRAGMA table_info({table})")
        }
        missing = required - columns
        if missing:
            raise ValueError(
                f"{table} is missing required column(s): "
                + ", ".join(sorted(missing))
            )


def load_source_files(
    connection: sqlite3.Connection,
    *,
    path_column: str,
    case_sensitive: bool,
) -> dict[str, list[SourceFile]]:
    grouped: DefaultDict[str, list[SourceFile]] = defaultdict(list)
    column = quote_identifier(path_column)

    query = f"SELECT id, {column} AS match_path, checksum FROM source_files"
    for row in connection.execute(query):
        source_id = int(row["id"])
        source_path = row["match_path"]

        if source_path is None or not str(source_path).strip():
            key = f"<missing-path:id={source_id}>"
            display_path = "<NULL>" if source_path is None else str(source_path)
        else:
            display_path = str(source_path)
            key = normalize_path(display_path, case_sensitive=case_sensitive)

        grouped[key].append(
            SourceFile(
                id=source_id,
                path=display_path,
                checksum=row["checksum"],
            )
        )

    return dict(grouped)


def checksum_set(rows: Iterable[SourceFile]) -> set[int | None]:
    return {row.checksum for row in rows}


def format_source_rows(rows: list[SourceFile]) -> str:
    return ", ".join(f"id={row.id}, checksum={row.checksum}" for row in rows)


def load_projects_by_path(
    connection: sqlite3.Connection,
    *,
    case_sensitive: bool,
) -> dict[str, list[sqlite3.Row]]:
    projects: DefaultDict[str, list[sqlite3.Row]] = defaultdict(list)

    for row in connection.execute("SELECT id, name, path FROM projects"):
        project_path = row["path"]
        if project_path is None or not str(project_path).strip():
            continue

        key = normalize_path(str(project_path), case_sensitive=case_sensitive)
        projects[key].append(row)

    return dict(projects)


def load_project_files_for_source(
    connection: sqlite3.Connection,
    source_file_id: int,
) -> list[ProjectFileMetadata]:
    query = """
        SELECT
            pf.id AS project_file_id,
            pf.project_id,
            p.name AS project_name,
            p.path AS project_path,
            pf.source_file_id,
            pf.timestamp,
            pf.build_md5sum
        FROM project_files AS pf
        JOIN projects AS p
          ON p.id = pf.project_id
        WHERE pf.source_file_id = ?
        ORDER BY p.path, pf.id
    """

    rows: list[ProjectFileMetadata] = []
    for row in connection.execute(query, (source_file_id,)):
        project_path = row["project_path"]
        if project_path is None:
            continue

        rows.append(
            ProjectFileMetadata(
                project_file_id=int(row["project_file_id"]),
                project_id=int(row["project_id"]),
                project_name=row["project_name"],
                project_path=str(project_path),
                source_file_id=int(row["source_file_id"]),
                timestamp=row["timestamp"],
                build_md5sum=row["build_md5sum"],
            )
        )

    return rows


def find_r1_project_file(
    connection: sqlite3.Connection,
    *,
    r1_project_id: int,
    r1_source_file_id: int,
) -> sqlite3.Row | None:
    rows = list(
        connection.execute(
            """
            SELECT id, timestamp, build_md5sum
            FROM project_files
            WHERE project_id = ?
              AND source_file_id = ?
            """,
            (r1_project_id, r1_source_file_id),
        )
    )

    if len(rows) > 1:
        raise ValueError(
            "Multiple project_files rows exist for "
            f"project_id={r1_project_id}, source_file_id={r1_source_file_id}"
        )

    return rows[0] if rows else None


def plan_updates(
    r1: sqlite3.Connection,
    r2: sqlite3.Connection,
    *,
    path_column: str,
    case_sensitive: bool,
) -> tuple[
    list[PlannedUpdate],
    list[str],
    list[str],
    list[str],
    list[str],
]:
    r1_sources = load_source_files(
        r1, path_column=path_column, case_sensitive=case_sensitive
    )
    r2_sources = load_source_files(
        r2, path_column=path_column, case_sensitive=case_sensitive
    )
    r1_projects = load_projects_by_path(r1, case_sensitive=case_sensitive)

    duplicate_source_paths: list[str] = []
    for key, rows in r1_sources.items():
        if len(rows) > 1:
            duplicate_source_paths.append(
                f"R1 duplicate source path {rows[0].path}: {format_source_rows(rows)}"
            )
    for key, rows in r2_sources.items():
        if len(rows) > 1:
            duplicate_source_paths.append(
                f"R2 duplicate source path {rows[0].path}: {format_source_rows(rows)}"
            )

    duplicate_project_paths = [
        rows[0]["path"]
        for rows in r1_projects.values()
        if len(rows) > 1
    ]

    changed_paths: list[str] = []
    skipped: list[str] = []
    plans: list[PlannedUpdate] = []

    common_keys = sorted(set(r1_sources) & set(r2_sources))
    for key in common_keys:
        if checksum_set(r1_sources[key]) == checksum_set(r2_sources[key]):
            continue

        changed_paths.append(r1_sources[key][0].path)

        # Ambiguous source paths are unsafe to update automatically.
        if len(r1_sources[key]) != 1 or len(r2_sources[key]) != 1:
            skipped.append(
                f"Skipped {r1_sources[key][0].path}: duplicate source path"
            )
            continue

        r1_source = r1_sources[key][0]
        r2_source = r2_sources[key][0]

        for r2_pf in load_project_files_for_source(r2, r2_source.id):
            project_key = normalize_path(
                r2_pf.project_path, case_sensitive=case_sensitive
            )
            matching_projects = r1_projects.get(project_key, [])

            if not matching_projects:
                skipped.append(
                    f"Skipped project {r2_pf.project_path!r} for "
                    f"{r2_source.path}: no matching R1 project path"
                )
                continue

            if len(matching_projects) > 1:
                skipped.append(
                    f"Skipped project {r2_pf.project_path!r} for "
                    f"{r2_source.path}: duplicate R1 project path"
                )
                continue

            r1_project = matching_projects[0]
            r1_pf = find_r1_project_file(
                r1,
                r1_project_id=int(r1_project["id"]),
                r1_source_file_id=r1_source.id,
            )

            if r1_pf is None:
                skipped.append(
                    f"Skipped project {r2_pf.project_path!r} for "
                    f"{r2_source.path}: no matching R1 project_files row"
                )
                continue

            if (
                r1_pf["timestamp"] == r2_pf.timestamp
                and r1_pf["build_md5sum"] == r2_pf.build_md5sum
            ):
                continue

            plans.append(
                PlannedUpdate(
                    source_path=r1_source.path,
                    project_path=r2_pf.project_path,
                    project_name=r2_pf.project_name,
                    r1_project_file_id=int(r1_pf["id"]),
                    r1_source_file_id=r1_source.id,
                    r2_source_file_id=r2_source.id,
                    old_timestamp=r1_pf["timestamp"],
                    new_timestamp=r2_pf.timestamp,
                    old_build_md5sum=r1_pf["build_md5sum"],
                    new_build_md5sum=r2_pf.build_md5sum,
                )
            )

    only_r1 = [
        r1_sources[key][0].path
        for key in sorted(set(r1_sources) - set(r2_sources))
    ]
    only_r2 = [
        r2_sources[key][0].path
        for key in sorted(set(r2_sources) - set(r1_sources))
    ]

    warnings = duplicate_source_paths[:]
    warnings.extend(
        f"R1 duplicate project path: {path}" for path in duplicate_project_paths
    )

    return plans, changed_paths, skipped, only_r1, only_r2 + warnings


def create_backup(database: Path, requested_path: Path | None) -> Path:
    backup = (
        requested_path
        if requested_path is not None
        else database.with_name(database.name + ".before_project_files_update.bak")
    )

    if backup.resolve() == database.resolve():
        raise ValueError("Backup path must be different from database1")

    shutil.copy2(database, backup)
    return backup


def apply_updates(database: Path, plans: list[PlannedUpdate]) -> int:
    updated = 0

    with closing(sqlite3.connect(database)) as connection:
        connection.execute("PRAGMA foreign_keys = ON")
        connection.execute("BEGIN IMMEDIATE")

        for plan in plans:
            cursor = connection.execute(
                """
                UPDATE project_files
                SET timestamp = ?,
                    build_md5sum = ?
                WHERE id = ?
                  AND (
                        timestamp IS NOT ?
                     OR build_md5sum IS NOT ?
                  )
                """,
                (
                    plan.new_timestamp,
                    plan.new_build_md5sum,
                    plan.r1_project_file_id,
                    plan.new_timestamp,
                    plan.new_build_md5sum,
                ),
            )

            if cursor.rowcount != 1:
                raise RuntimeError(
                    "Expected to update exactly one project_files row, but "
                    f"updated {cursor.rowcount} for id={plan.r1_project_file_id}"
                )
            updated += 1

        integrity = connection.execute("PRAGMA integrity_check").fetchone()[0]
        if integrity != "ok":
            raise RuntimeError(f"SQLite integrity check failed: {integrity}")

        connection.commit()

    return updated


def print_report(
    database1: Path,
    database2: Path,
    *,
    path_column: str,
    case_sensitive: bool,
    plans: list[PlannedUpdate],
    changed_paths: list[str],
    skipped: list[str],
    only_r1: list[str],
    other_warnings: list[str],
    applying: bool,
) -> None:
    print(f"Target/base database (R1): {database1}")
    print(f"Source/new database (R2):  {database2}")
    print(f"Source path column:         {path_column}")
    print(f"Case-sensitive matching:    {case_sensitive}")
    print(f"Mode:                       {'APPLY' if applying else 'DRY RUN'}")
    print()
    print(f"Changed source paths:       {len(changed_paths)}")
    print(f"Planned row updates:        {len(plans)}")
    print(f"Skipped R2 project rows:    {len(skipped)}")
    print(f"Only in R1:                 {len(only_r1)}")
    print(f"Other warnings:             {len(other_warnings)}")

    if changed_paths:
        print("\nCHANGED SOURCE CHECKSUMS")
        for path in changed_paths:
            print(f"  {path}")

    if plans:
        print("\nPROJECT_FILES UPDATES")
        for plan in plans:
            project_label = (
                f"{plan.project_name} [{plan.project_path}]"
                if plan.project_name
                else plan.project_path
            )
            print(f"  Source:  {plan.source_path}")
            print(f"  Project: {project_label}")
            print(
                f"    source_file_id: R2={plan.r2_source_file_id} "
                f"-> R1={plan.r1_source_file_id}"
            )
            print(f"    R1 project_files.id: {plan.r1_project_file_id}")
            print(
                f"    timestamp:    {plan.old_timestamp} "
                f"-> {plan.new_timestamp}"
            )
            print(
                f"    build_md5sum: {plan.old_build_md5sum} "
                f"-> {plan.new_build_md5sum}"
            )

    if skipped:
        print("\nSKIPPED")
        for message in skipped:
            print(f"  {message}")

    if only_r1:
        print("\nSOURCE PATHS ONLY IN R1")
        for path in only_r1:
            print(f"  {path}")

    if other_warnings:
        print("\nWARNINGS / SOURCE PATHS ONLY IN R2")
        for message in other_warnings:
            print(f"  {message}")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description=(
            "Compare source_files checksums by path and copy matching "
            "project_files.timestamp/build_md5sum values from database2 "
            "into database1."
        )
    )
    parser.add_argument(
        "database1",
        type=Path,
        help="R1 base/target SQLite database",
    )
    parser.add_argument(
        "database2",
        type=Path,
        help="R2 newer/source SQLite database",
    )
    parser.add_argument(
        "--path-column",
        choices=("path", "display_path", "relative_path"),
        default="path",
        help="source_files column used to match rows (default: path)",
    )
    parser.add_argument(
        "--case-sensitive",
        action="store_true",
        help="Treat source and project path letter case as significant",
        default=False
    )
    parser.add_argument(
        "--apply",
        action="store_true",
        help="Actually update database1; otherwise perform a dry run",
        default=False
    )
    parser.add_argument(
        "--backup",
        type=Path,
        help=(
            "Backup filename used with --apply. By default, creates "
            "<database1>.before_project_files_update.bak"
        ),
        default = None
    )
    parser.add_argument(
        "--no-backup",
        action="store_true",
        help="Do not create a backup before --apply",
        default = False
    )

    args = parser.parse_args()
    if args.backup is not None and args.no_backup:
        parser.error("--backup and --no-backup cannot be used together")
    if args.backup is not None and not args.apply:
        parser.error("--backup requires --apply")

    return args


def run (database1, database2, apply, path_column = 'path', case_sensitive = False,  no_backup = True, backup = None, verbose = False):
    
    from pathlib import Path
    
    if isinstance(database1, str):
        db1 = Path(database1)
    else:
        db1 = database1
        
    if isinstance(database2, str):
        db2 = Path(database2)
    else:
        db2 = database2

    for database in (db1, db2):
        if not database.is_file():
            print(f"Error: database does not exist: {database}", file=sys.stderr)
            return 2

    try:
        with closing(connect_read_only(db1)) as r1, \
             closing(connect_read_only(db2)) as r2:
                 
            validate_schema(r1, path_column)
            validate_schema(r2, path_column)
            (
                plans,
                changed_paths,
                skipped,
                only_r1,
                other_warnings,
            ) = plan_updates(
                r1,
                r2,
                path_column=path_column,
                case_sensitive=case_sensitive,
            )

        if verbose:
            print_report(
                db1,
                db2,
                path_column=path_column,
                case_sensitive=case_sensitive,
                plans=plans,
                changed_paths=changed_paths,
                skipped=skipped,
                only_r1=only_r1,
                other_warnings=other_warnings,
                applying=apply,
            )

        if not apply:
            if plans:
                print(
                    "\nDry run only. Re-run with --apply to update database1."
                )
            return 1 if (changed_paths or skipped or only_r1 or other_warnings) else 0

        if not plans:
            print("\nNo project_files rows require updating.")
            return 1 if (skipped or only_r1 or other_warnings) else 0

        updated = apply_updates(db1, plans)
        
        if verbose:
            print(f"Updated {updated} project_files row(s) in {db1}")

        return 1 if (skipped or only_r1 or other_warnings) else 0

    except (sqlite3.Error, OSError, ValueError, RuntimeError) as error:
        print(f"Error: {error}", file=sys.stderr)
        return 2

def main() -> int:
    args = parse_args()
    run(args.database1, args.database2, args.apply,
        args.path_column, args.case_sensitive, 
        args.no_backup, args.backup)
    
if __name__ == "__main__":
    raise SystemExit(main())
