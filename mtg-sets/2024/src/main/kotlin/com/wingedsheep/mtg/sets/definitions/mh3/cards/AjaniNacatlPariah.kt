package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ajani, Nacatl Pariah // Ajani, Nacatl Avenger — Modern Horizons 3 #237
 * {1}{W} · Legendary Creature — Cat Warrior 1/2 // Legendary Planeswalker — Ajani (loyalty 3)
 *
 * Modeling notes:
 *  - "One or more other Cats you control die" is the batched death trigger
 *    ([Triggers.oneOrMoreOther]), so a board wipe of Cats triggers once, not per Cat.
 *  - The flip is [Effects.ExileAndReturnTransformed] behind a "may": a new object enters back face
 *    up with its printed loyalty 3.
 *  - The 0: "When you do, if you control a red permanent other than Ajani, …" is a reflexive
 *    trigger (CR 603.12) with an intervening "if": it only triggers when the condition holds after
 *    the token is made, and the condition is checked again as it resolves. The reflexive's target
 *    is chosen when it goes on the stack, not on activation (ruling). Damage equals the number of
 *    creatures you control as the reflexive resolves.
 *  - The −4 is gather → one pick per listed type (artifact / creature / enchantment /
 *    planeswalker) → sacrifice the rest. Lands are never gathered, so they're never sacrificed;
 *    `chooseOnePerCategory` walks each opponent in APNAP order.
 */
private val catsYouControl = GameObjectFilter.Creature.withSubtype("Cat").youControl()

private val redPermanentOtherThanAjani =
    Conditions.YouControl(GameObjectFilter.Permanent.withColor(Color.RED), excludeSelf = true)

private const val CAT_WARRIOR_TOKEN_IMAGE =
    "https://cards.scryfall.io/normal/front/c/e/ce5c5bcf-1fdd-4d73-a92b-223292da00ca.jpg?1783911117"

private fun createCatWarrior() = Effects.CreateToken(
    power = 2,
    toughness = 1,
    colors = setOf(Color.WHITE),
    creatureTypes = setOf("Cat", "Warrior"),
    imageUri = CAT_WARRIOR_TOKEN_IMAGE,
)

private val AjaniNacatlPariahFront = card("Ajani, Nacatl Pariah") {
    manaCost = "{1}{W}"
    colorIdentity = "RW"
    typeLine = "Legendary Creature — Cat Warrior"
    power = 1
    toughness = 2
    oracleText = "When Ajani enters, create a 2/1 white Cat Warrior creature token.\n" +
        "Whenever one or more other Cats you control die, you may exile Ajani, then return him to " +
        "the battlefield transformed under his owner's control."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = createCatWarrior()
        description = "When Ajani enters, create a 2/1 white Cat Warrior creature token."
    }

    triggeredAbility {
        trigger = Triggers.oneOrMoreOther(catsYouControl).die()
        effect = Effects.May(Effects.ExileAndReturnTransformed(EffectTarget.Self))
        description = "Whenever one or more other Cats you control die, you may exile Ajani, then " +
            "return him to the battlefield transformed under his owner's control."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "237"
        artist = "Chris Rallis"
        flavorText = "His pride denied him; his brother did not."
        imageUri = "https://cards.scryfall.io/normal/front/0/d/0d16e8e0-31b2-4389-afd6-783c501f6fa0.jpg?1783911238"

        ruling("2024-06-07", "In some rare cases, a spell or ability may cause Ajani, Nacatl Pariah to transform while it's a creature (front face up) on the battlefield. If this happens, Ajani, Nacatl Avenger won't have any loyalty counters on him and will subsequently be put into his owner's graveyard.")
    }
}

private val AjaniNacatlAvenger = card("Ajani, Nacatl Avenger") {
    manaCost = ""
    colorIdentity = "RW"
    colorIndicator = "RW"
    typeLine = "Legendary Planeswalker — Ajani"
    startingLoyalty = 3
    oracleText = "+2: Put a +1/+1 counter on each Cat you control.\n" +
        "0: Create a 2/1 white Cat Warrior creature token. When you do, if you control a red " +
        "permanent other than Ajani, he deals damage equal to the number of creatures you control " +
        "to any target.\n" +
        "−4: Each opponent chooses an artifact, a creature, an enchantment, and a planeswalker from " +
        "among the nonland permanents they control, then sacrifices the rest."

    loyaltyAbility(+2) {
        effect = Effects.ForEachInGroup(
            filter = GroupFilter(catsYouControl),
            effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.IterationEntity),
        )
        description = "Put a +1/+1 counter on each Cat you control."
    }

    loyaltyAbility(0) {
        effect = createCatWarrior() then Effects.If(
            redPermanentOtherThanAjani,
            Effects.ReflexiveTrigger(
                action = Effects.Nothing,
                optional = false,
                descriptionOverride = "When you do, if you control a red permanent other than Ajani, " +
                    "he deals damage equal to the number of creatures you control to any target.",
            ) {
                val anyTarget = target(Targets.Any)
                effect = Effects.If(
                    redPermanentOtherThanAjani,
                    Effects.DealDamage(DynamicAmounts.creaturesYouControl(), anyTarget),
                )
            },
        )
        description = "Create a 2/1 white Cat Warrior creature token. When you do, if you control a " +
            "red permanent other than Ajani, he deals damage equal to the number of creatures you " +
            "control to any target."
    }

    loyaltyAbility(-4) {
        effect = Effects.Pipeline {
            val atRisk = gather(GameObjectFilter.NonlandPermanent.opponentControls())
            val kept = chooseOnePerCategory(
                atRisk,
                listOf(
                    GameObjectFilter.Artifact,
                    GameObjectFilter.Creature,
                    GameObjectFilter.Enchantment,
                    GameObjectFilter.Planeswalker,
                ),
            )
            sacrifice(exclude(atRisk, kept))
        }
        description = "Each opponent chooses an artifact, a creature, an enchantment, and a " +
            "planeswalker from among the nonland permanents they control, then sacrifices the rest."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "237"
        artist = "Chris Rallis"
        imageUri = "https://cards.scryfall.io/normal/back/0/d/0d16e8e0-31b2-4389-afd6-783c501f6fa0.jpg?1783911238"

        ruling("2024-06-07", "You can activate one of Ajani, Nacatl Avenger's loyalty abilities the turn he enters the battlefield. However, you may do so only during one of your main phases when the stack is empty. For example, if Ajani, Nacatl Avenger enters the battlefield during combat, there will be an opportunity for your opponent to remove him before you can activate one of his abilities.")
        ruling("2024-06-07", "You don't choose a target for Ajani, Nacatl Avenger's second ability when you activate it. Rather, if you control a red permanent other than Ajani, Nacatl Avenger, a second \"reflexive\" ability triggers when you create a Cat Warrior creature token this way. You choose a target for that ability as it goes on the stack. Each player may respond to this triggered ability as normal.")
        ruling("2024-06-07", "When Ajani, Nacatl Avenger's last ability resolves, starting with the next opponent in turn order, each opponent in turn order chooses permanents they control. Each opponent will know the choices made by players who chose before them. They then sacrifice all of their unchosen nonland permanents simultaneously.")
        ruling("2024-06-07", "A permanent with more than one type may be chosen as any of its types. For example, an artifact creature may be chosen as the artifact, the creature, or both. Choosing the same permanent twice this way has the same result as choosing it once.")
        ruling("2024-06-07", "Lands can't be chosen and won't be sacrificed, even if they have any of the types referenced by Ajani, Nacatl Avenger's last ability.")
        ruling("2024-06-07", "If an opponent doesn't control any permanents of one of the types, they'll still choose the permanents of the types they do control.")
    }
}

val AjaniNacatlPariah: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = AjaniNacatlPariahFront,
    backFace = AjaniNacatlAvenger,
)
