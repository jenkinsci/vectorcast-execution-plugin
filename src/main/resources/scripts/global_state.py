#global_state.py

# global_state.py
class GlobalState:
    _instance = None

    def __new__(cls):
        if cls._instance is None:
            cls._instance = super().__new__(cls)
            cls._instance.fullMpName = ""
            cls._instance.msgs = []
        return cls._instance

globalState = GlobalState()
