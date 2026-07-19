package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode

/**
 * Ant-Man's Army — Marvel Super Heroes #161
 * {2}{G} · Creature — Insect · Common
 * 3/2
 *
 * When this creature enters, create a Food token or a Treasure token.
 *
 * "X or Y" on a triggered ability is a resolution-time choice between two token modes — a
 * [ModalEffect.chooseOne] with `countsAsModalSpell = false` (this is not a printed
 * "choose one —" modal *spell*, so it must not feed modal-spell-matters checks).
 */
val AntMansArmy = card("Ant-Man's Army") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Insect"
    power = 3
    toughness = 2
    oracleText = "When this creature enters, create a Food token or a Treasure token. " +
        "(A Food token is an artifact with \"{2}, {T}, Sacrifice this token: You gain 3 life.\" " +
        "A Treasure token is an artifact with \"{T}, Sacrifice this token: Add one mana of any " +
        "color.\")"

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = ModalEffect.chooseOne(
            Mode.noTarget(Effects.CreateFood(), "Create a Food token"),
            Mode.noTarget(Effects.CreateTreasure(), "Create a Treasure token"),
            countsAsModalSpell = false
        )
        description = "When this creature enters, create a Food token or a Treasure token."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "161"
        artist = "Bachzim"
        flavorText = "\"C'mon girls, we're not here for snacks. Okay, maybe just one.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/1/61a89566-06c1-404f-8217-e5276e2dc5a2.jpg?1783902921"
    }
}
