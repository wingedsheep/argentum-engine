package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Hour of Glory — Hour of Devastation #65
 * {3}{B} · Instant
 *
 * Exile target creature. If that creature was a God, its controller reveals their hand and exiles
 * all cards from it with the same name as that creature.
 *
 * The God test (projected subtype) runs before anything moves, so it sees the creature as it last
 * existed on the battlefield. On the God branch, that player's hand and its same-named cards are
 * gathered while the creature is still on the battlefield — the name is read off projected state,
 * so a copy of a God matches by the God's name rather than the card it is in exile — and only then
 * is the creature exiled, the hand revealed, and the matches exiled. An illegal target fizzles the
 * whole spell, so no hand is revealed (2017-07-14 ruling).
 */
val HourOfGlory = card("Hour of Glory") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Exile target creature. If that creature was a God, its controller reveals their hand " +
        "and exiles all cards from it with the same name as that creature."

    spell {
        val creature = target(TargetFilter.Creature)
        val thatPlayer = Player.ControllerOf("target creature")
        effect = Effects.If(
            condition = Conditions.TargetMatchesFilter(GameObjectFilter.Creature.withSubtype(Subtype.GOD), creature),
            then = Effects.Pipeline {
                val hand = gather(CardSource.FromZone(Zone.HAND, player = thatPlayer))
                val sameName = filter(
                    hand,
                    GameObjectFilter.Any.sharingNameWith(creature),
                )
                run(Effects.Exile(creature))
                reveal(hand)
                exile(sameName, owner = thatPlayer)
            },
            otherwise = Effects.Exile(creature),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "65"
        artist = "Svetlin Velinov"
        flavorText = "The gods of the trials also faced the opportunity to prove themselves worthy."
        imageUri = "https://cards.scryfall.io/normal/front/f/3/f35e66cb-af50-411a-b6f2-16fb7072eff5.jpg?1783936040"
        ruling(
            "2017-07-14",
            "If the target creature is an illegal target by the time Hour of Glory resolves, the entire " +
                "spell doesn't resolve. No player reveals their hand or exiles cards from it.",
        )
    }
}
