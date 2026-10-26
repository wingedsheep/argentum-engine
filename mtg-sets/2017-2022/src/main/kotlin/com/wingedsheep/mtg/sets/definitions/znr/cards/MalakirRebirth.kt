package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Malakir Rebirth {B} // Malakir Mire — Zendikar Rising #111
 * Instant // Land · Modal double-faced card
 *
 * Front — Malakir Rebirth: "Choose target creature. You lose 2 life. Until end of turn, that
 * creature gains 'When this creature dies, return it to the battlefield tapped under its owner's
 * control.'"
 * Back — Malakir Mire: "This land enters tapped. {T}: Add {B}."
 *
 * The front is the Infuse with Vitality shape: an end-of-turn granted self dies-trigger that moves
 * the card graveyard → battlefield tapped under its owner's control (`fromZone = GRAVEYARD` makes it
 * a no-op if the card already left the graveyard). The life loss is part of the targeted spell, so
 * when the target is illegal on resolution the spell doesn't resolve and no life is lost (ruling).
 */
private val MalakirRebirthFront = card("Malakir Rebirth") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Choose target creature. You lose 2 life. Until end of turn, that creature gains " +
        "\"When this creature dies, return it to the battlefield tapped under its owner's control.\""

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.LoseLife(2, EffectTarget.Controller) then
            Effects.GrantTriggeredAbility(
                ability = TriggeredAbility.create(
                    trigger = Triggers.self.dies(),
                    effect = Effects.Move(
                        target = EffectTarget.Self,
                        destination = Zone.BATTLEFIELD,
                        placement = ZonePlacement.Tapped,
                        fromZone = Zone.GRAVEYARD
                    ),
                    descriptionOverride = "When this creature dies, return it to the battlefield " +
                        "tapped under its owner's control."
                ),
                target = creature,
                duration = Duration.EndOfTurn
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "111"
        artist = "Marta Nael"
        flavorText = "\"Those who fail to escape Malakir become part of the city's defense.\"\n—Doros, expedition guide"
        imageUri = "https://cards.scryfall.io/normal/front/6/0/609d3ecf-f88d-4268-a8d3-4bf2bcf5df60.jpg?1783929378"
        ruling(
            "2020-09-25",
            "If the target creature is an illegal target by the time Malakir Rebirth tries to resolve, " +
                "the spell doesn't resolve. You don't lose 2 life."
        )
        ruling(
            "2020-09-25",
            "Malakir Rebirth's effect works only once. If the targeted creature dies and is then returned " +
                "to the battlefield, it's considered to be a new creature. If that new creature dies, it " +
                "won't come back."
        )
    }
}

private val MalakirMireBack = card("Malakir Mire") {
    typeLine = "Land"
    colorIdentity = "B"
    oracleText = "This land enters tapped.\n{T}: Add {B}."

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.BLACK)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "111"
        artist = "Marta Nael"
        flavorText = "\"Getting into Malakir isn't difficult. It's getting out that's the hard part.\"\n—Doros, expedition guide"
        imageUri = "https://cards.scryfall.io/normal/back/6/0/609d3ecf-f88d-4268-a8d3-4bf2bcf5df60.jpg?1783929378"
    }
}

val MalakirRebirth: CardDefinition = CardDefinition.modalDoubleFacedLand(
    frontFace = MalakirRebirthFront,
    backFace = MalakirMireBack,
)
