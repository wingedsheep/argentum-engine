package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword

/**
 * Add For Mirrodin! (Phyrexia: All Will Be One) — keyword + enters-the-battlefield triggered ability.
 *
 * "For Mirrodin! (When this Equipment enters, create a 2/2 red Rebel creature token, then
 * attach this to it.)"
 *
 * The same create-then-attach shell as [jobSelect], making a 2/2 red Rebel. The token carries no
 * image of its own: its art resolves from the printing set's token sheet (ONE's `tone` Rebel).
 * The per-card equip cost and equipped-creature bonus are authored on the card alongside this call.
 */
fun CardBuilder.forMirrodin() = equipmentMakesItsOwnBearer(
    keyword = Keyword.FOR_MIRRODIN,
    token = Effects.CreateToken(
        power = 2,
        toughness = 2,
        colors = setOf(Color.RED),
        creatureTypes = setOf("Rebel")
    ),
    reminderText = "For Mirrodin! (When this Equipment enters, create a 2/2 red Rebel " +
        "creature token, then attach this to it.)"
)
