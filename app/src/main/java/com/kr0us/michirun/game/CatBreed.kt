package com.kr0us.michirun.game

import com.kr0us.michirun.R

/** Las razas cambian el aspecto, nunca la física ni la dificultad. */
enum class CatBreed(val titleRes: Int, val traitRes: Int, val fur: Int, val markings: Int, val eyes: Int) {
    SIAMESE(R.string.siamese, R.string.siamese_trait, 0xFFE1D3B8.toInt(), 0xFF665043.toInt(), 0xFF468EA5.toInt()),
    GRAY(R.string.gray, R.string.gray_trait, 0xFF9AA5A5.toInt(), 0xFF596968.toInt(), 0xFFE5BF59.toInt()),
    PERSIAN(R.string.persian, R.string.persian_trait, 0xFFEDE4D6.toInt(), 0xFFC6B9AD.toInt(), 0xFF659F8D.toInt()),
    TABBY(R.string.orange, R.string.orange_trait, 0xFFE0A159.toInt(), 0xFFAA6535.toInt(), 0xFF65866A.toInt()),
    TUXEDO(R.string.tuxedo, R.string.tuxedo_trait, 0xFF444849.toInt(), 0xFFF2EBDF.toInt(), 0xFFB7C86B.toInt()),
    CALICO(R.string.calico, R.string.calico_trait, 0xFFEDE4D6.toInt(), 0xFFC38652.toInt(), 0xFF729C7B.toInt());

    companion object {
        fun fromId(id: String?): CatBreed = entries.firstOrNull { it.name == id } ?: SIAMESE
    }
}
