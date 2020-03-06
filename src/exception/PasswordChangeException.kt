package exception

import java.lang.RuntimeException

class PasswordChangeException(message: String): RuntimeException(message)