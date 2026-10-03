package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate

/**
 * Bridgeworks Battle {2}{G} // Tanglespan Bridgeworks
 * Sorcery
 * Target creature you control gets +2/+2 until end of turn. It fights up to one target creature you
 * don't control. (Each deals damage equal to its power to the other.)
 * //
 * Land
 * As this land enters, you may pay 3 life. If you don't, it enters tapped.
 * {T}: Add {G}.
 *
 * "You don't control" is `Not(ControlledByYou)` rather than `opponentControls()` — the two agree in a
 * duel and separate in Two-Headed Giant, where a teammate's creature is one you don't control. The
 * second target is optional ("up to one"), so the spell can be cast targeting only your creature.
 */
private val BridgeworksBattleFront = card("Bridgeworks Battle") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Target creature you control gets +2/+2 until end of turn. It fights up to one target " +
        "creature you don't control. (Each deals damage equal to its power to the other.)"

    spell {
        val yourCreature = target(TargetFilter.CreatureYouControl)
        val theirCreature = target(
            TargetFilter(
                GameObjectFilter.Creature.withControllerPredicate(
                    ControllerPredicate.Not(ControllerPredicate.ControlledByYou)
                )
            ),
            optional = true,
        )
        effect = Effects.ModifyStats(2, 2, yourCreature) then Effects.Fight(yourCreature, theirCreature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "249"
        artist = "Ron Spears"
        flavorText = "\"I thought trolls were supposed to be *under* bridges!\"\n—Nelvin, Vantress squire"
        imageUri = "https://cards.scryfall.io/normal/front/e/b/ebef3db0-2b58-4581-a79c-fbca9a059e63.jpg?1783911228"
        ruling("2024-06-07", "You can cast Bridgeworks Battle targeting only the creature you control.")
        ruling("2024-06-07", "If you choose two target creatures and either target is an illegal target as Bridgeworks Battle tries to resolve, neither creature will deal or be dealt damage.")
        ruling("2024-06-07", "If the creature you control is an illegal target as Bridgeworks Battle tries to resolve, no creature gets +2/+2. If that creature is a legal target but the creature you don't control isn't, the creature you control still gets +2/+2.")
    }
}

private val TanglespanBridgeworksBack = card("Tanglespan Bridgeworks") {
    typeLine = "Land"
    colorIdentity = "G"
    oracleText = "As this land enters, you may pay 3 life. If you don't, it enters tapped.\n{T}: Add {G}."

    replacementEffect(EntersTapped(payLifeCost = 3))

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.GREEN)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "249"
        artist = "Ron Spears"
        flavorText = "The chasm formed during the Phyrexian Invasion quickly filled with new life."
        imageUri = "https://cards.scryfall.io/normal/back/e/b/ebef3db0-2b58-4581-a79c-fbca9a059e63.jpg?1783911228"
    }
}

val BridgeworksBattle: CardDefinition = CardDefinition.modalDoubleFacedLand(
    frontFace = BridgeworksBattleFront,
    backFace = TanglespanBridgeworksBack,
)
