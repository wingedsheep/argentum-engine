package com.wingedsheep.mtg.sets.definitions.clb.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Rug of Smothering
 * {3}
 * Artifact Creature — Construct
 * 1/3
 * Flying
 * Whenever a player casts a spell, they lose 1 life for each spell they've cast this turn.
 *
 * The count reads the caster's per-turn cast history, which already includes the spell that
 * triggered the ability.
 */
val RugOfSmothering = card("Rug of Smothering") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Construct"
    power = 1
    toughness = 3
    oracleText = "Flying\nWhenever a player casts a spell, they lose 1 life for each spell they've cast this turn."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.anyPlayer.casts()
        effect = Effects.LoseLife(
            amount = DynamicAmounts.spellsCastThisTurn(Player.TriggeringPlayer),
            target = EffectTarget.PlayerRef(Player.TriggeringPlayer)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "336"
        artist = "Ioannis Fiore"
        flavorText = "\"No, not an accessory to murder—I'm looking for an accessory *for* murder.\"\n" +
            "—Nadja, Flaming Fist investigator"
        imageUri = "https://cards.scryfall.io/normal/front/a/7/a73d1cb0-d0dc-4f2a-9cf2-954d5889dd08.jpg?1783922664"
    }
}
