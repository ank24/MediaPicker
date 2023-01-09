package com.ankit.mediapicker

/**
 * Description: This method is used for the purpose of reducing same try catch blocks. Using inline function also saves the memory.
 * @author Ankit Mishra
 * @param function Provide function for which the try/catch is required.
 * @since 08th Jan, 2022
 * @return Unit
 */
inline fun ifThrowsPrintStackTrace(function: () -> Unit) {
    try {
        function()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
