package com.wingedsheep.mtg.sets.definitions.tor.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Chainer, Dementia Master
 * {3}{B}{B}
 * Legendary Creature — Human Minion
 * 3/3
 *
 * All Nightmares get +1/+1.
 * {B}{B}{B}, Pay 3 life: Put target creature card from a graveyard onto the battlefield under your
 * control. That creature is black and is a Nightmare in addition to its other creature types.
 * When Chainer leaves the battlefield, exile all Nightmares.
 *
 * The reanimation is the Dread Slaver shape, except the color is *replaced* ([Effects.ChangeColor],
 * "black only" per the ruling) rather than added; both rewrites are permanent and independent of
 * Chainer, so the creature stays a black Nightmare after Chainer leaves — and is then exiled by the
 * leaves trigger, which reads projected subtypes and so sees every Nightmare, printed or granted.
 */
val ChainerDementiaMaster = card("Chainer, Dementia Master") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Human Minion"
    power = 3
    toughness = 3
    oracleText = "All Nightmares get +1/+1.\n" +
        "{B}{B}{B}, Pay 3 life: Put target creature card from a graveyard onto the battlefield under " +
        "your control. That creature is black and is a Nightmare in addition to its other creature types.\n" +
        "When Chainer leaves the battlefield, exile all Nightmares."

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(GameObjectFilter.Creature.withSubtype("Nightmare"))
        )
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{B}{B}{B}"), Costs.PayLife(3))
        val creatureCard = target(TargetFilter.CreatureInGraveyard)
        effect = Effects.PutOntoBattlefieldUnderYourControl(creatureCard) then
            Effects.ChangeColor(creatureCard, setOf(Color.BLACK), Duration.Permanent) then
            Effects.AddCreatureType("Nightmare", creatureCard, Duration.Permanent)
        description = "Put target creature card from a graveyard onto the battlefield under your " +
            "control. That creature is black and is a Nightmare in addition to its other creature types."
    }

    triggeredAbility {
        trigger = Triggers.self.leaves()
        effect = Effects.Pipeline {
            exile(gather(GameObjectFilter.Permanent.withSubtype("Nightmare")))
        }
        description = "When Chainer leaves the battlefield, exile all Nightmares."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "56"
        artist = "Mark Zug"
        imageUri = "https://cards.scryfall.io/normal/front/4/0/40342b11-1005-4ccc-bef4-9ea4c640b048.jpg?1783945158"
        ruling("2022-12-08", "The creature put onto the battlefield by Chainer's activated ability is black only, not black in addition to its other colors. It is, however, a Nightmare in addition to its other creature types. As it enters the battlefield, it is already a black Nightmare (and perhaps some other creature types); it doesn't enter and then become a black Nightmare.")
    }
}
