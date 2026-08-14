package com.wingedsheep.mtg.sets.definitions.pls.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.conditions.WasKicked
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.TargetPlayer

/**
 * Orim's Chant
 * {W}
 * Instant
 * Kicker {W}
 *
 * Target player can't cast spells this turn. If this spell was kicked, creatures can't
 * attack this turn.
 *
 * The "creatures can't attack" half is global (both players' creatures), matching the
 * printed text — it's not scoped to the targeted player's side.
 */
val OrimsChant = card("Orim's Chant") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Kicker {W} (You may pay an additional {W} as you cast this spell.)\n" +
        "Target player can't cast spells this turn. If this spell was kicked, creatures can't " +
        "attack this turn."

    keywordAbility(KeywordAbility.kicker("{W}"))

    spell {
        val opponent = target("target player", TargetPlayer())
        effect = Effects.CantCastSpells(opponent) then
            ConditionalEffect(
                condition = WasKicked,
                effect = Effects.CantAttackGroup(GroupFilter.AllCreatures),
            )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "11"
        artist = "Kev Walker"
        imageUri = "https://cards.scryfall.io/normal/front/0/5/055afa78-b969-498f-a3ad-c792426e5ee6.jpg?1783945629"
        ruling("2024-06-07", "Orim's Chant also won't affect creatures that are already attacking. It does not remove them from combat.")
        ruling("2024-06-07", "The target opponent can still activate abilities, including abilities of cards in their hands (like cycling). Their triggered abilities work as normal, they can still play lands, and so on.")
        ruling("2024-06-07", "Orim's Chant won't affect spells that your opponents cast before you cast Orim's Chant, including any spells that are still on the stack. Orim's Chant also won't stop your opponents from casting spells after you cast Orim's Chant but before Orim's Chant resolves.")
    }
}
