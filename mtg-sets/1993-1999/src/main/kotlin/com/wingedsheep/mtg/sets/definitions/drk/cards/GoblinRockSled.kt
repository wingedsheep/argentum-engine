package com.wingedsheep.mtg.sets.definitions.drk.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttackUnless
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Goblin Rock Sled
 * {1}{R}
 * Creature — Goblin
 * 3/1
 * Trample
 * This creature doesn't untap during your untap step if it attacked during your last turn.
 * This creature can't attack unless defending player controls a Mountain.
 *
 * The untap clause is *conditional*, so it is a `ConditionalStaticAbility` wrapping a
 * `GrantKeyword(DOESNT_UNTAP)` rather than the bare flag: the Sled untaps normally on a turn it
 * didn't attack, and a flag set once could not express that. The projector re-evaluates the gate
 * every untap step, which is exactly the printed behaviour — attack, sit out a turn, attack again.
 *
 * The attack restriction is the same [CantAttackUnless] shape as Island Fish Jasconius, aimed at the
 * *defending* player's Mountains.
 */
val GoblinRockSled = card("Goblin Rock Sled") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin"
    power = 3
    toughness = 1
    oracleText = "Trample\n" +
        "This creature doesn't untap during your untap step if it attacked during your last turn.\n" +
        "This creature can't attack unless defending player controls a Mountain."

    keywords(Keyword.TRAMPLE)

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(AbilityFlag.DOESNT_UNTAP.name, GroupFilter.source()),
            condition = Conditions.SourceAttackedLastTurn,
        )
    }

    staticAbility {
        ability = CantAttackUnless(Conditions.DefendingPlayerControlsLandType(Subtype.MOUNTAIN.value))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "67"
        artist = "Dennis Detwiller"
        imageUri = "https://cards.scryfall.io/normal/front/9/1/91e0b59d-8f9b-4a76-9845-bcb0dc32523d.jpg?1783947935"
    }
}
