package com.fildy.data.model

/** A change to the count of one category in one paddock, without moving animals elsewhere. */
sealed interface HerdAdjustment {
    /** Adds animals (births, purchases…). [headCount] > 0. */
    data class Add(val headCount: Int) : HerdAdjustment

    /** Removes animals (sales, deaths…). [headCount] > 0 and at most what the paddock has. */
    data class Remove(val headCount: Int) : HerdAdjustment

    /** Sets the count to [headCount] (≥ 0), e.g. after counting the animals again. */
    data class Set(val headCount: Int) : HerdAdjustment
}
