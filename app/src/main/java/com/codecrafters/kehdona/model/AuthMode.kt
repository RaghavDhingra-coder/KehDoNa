package com.codecrafters.kehdona.model

import androidx.annotation.Keep

/** The two ways a person enters KehDoNa. */
@Keep // Used as a navigation argument; keeps its serializer intact in minified builds.
enum class AuthMode { SignUp, LogIn }
