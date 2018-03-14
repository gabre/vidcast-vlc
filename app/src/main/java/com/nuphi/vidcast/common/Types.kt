package com.nuphi.vidcast.common

import arrow.core.Either
import arrow.core.Left
import arrow.core.None
import arrow.core.Option

/**
 * Created by gabre on 2/2/18.
 */
 object Types {
    data class VidCastError(val description: String, val t: Option<Throwable> = None)
    fun viderror(description: String): VidCastError {
        return VidCastError(description)
    }
    fun <O>videither(description: String): Either<VidCastError, O> {
        return Left(error(description))
    }
}