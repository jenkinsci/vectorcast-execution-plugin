package com.vectorcast.plugins.vectorcastexecution


class VectorCASTHelpersImpl {

    def script

    VectorCASTHelpersImpl(script) { this.script = script }

    // ===============================================================
    //
    // Function : fixUpName
    // Inputs   : command list
    // Action   : Fixup name so it doesn't include / or %## or any other special characters
    // Returns  : Fixed up name
    // Notes    : Used widely
    //
    // ===============================================================
    def fixUpName(name) {

        name = name ?: ""

        return (name.replace("/", "_")
                .replaceAll('\\%..', '_')
                .replaceAll('\\W', '_'))
    }

    // ===============================================================
    //
    // Function : getMpName
    // Inputs   : None
    // Action   : Returns the base name
    // Notes    : Used for creating report name
    //
    // ===============================================================

    def getMpName(mpNameInput) {
        // get the manage projects full name and base name
        def mpFullName = mpNameInput.split("/")[-1]
        def mpName = ""
        if (mpFullName.toLowerCase().endsWith(".vcm")) {
            mpName = mpFullName.take(mpFullName.lastIndexOf('.'))
        } else {
            mpName = mpFullName
        }
        return mpName
    }
}
