package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.values.DynamicAmount

private const val BORBORYGMOS_TRIGGER_TEXT =
    "Whenever Borborygmos and Fblthp enters or attacks, draw a card, then you may discard any " +
        "number of land cards. When you discard one or more cards this way, Borborygmos and " +
        "Fblthp deals twice that much damage to target creature."

/**
 * Draw a card, then discard any number of land cards from hand (zero allowed). Only if at least one
 * was discarded does the CR 603.12 reflexive trigger exist; its "target creature" is chosen as it
 * goes on the stack, and "twice that much" reads the discarded pile carried onto the reflexive.
 */
private fun borborygmosTriggerEffect(): Effect = Effects.Pipeline {
    run(Effects.DrawCards(1))
    val lands = gather(CardSource.FromZone(Zone.HAND, filter = GameObjectFilter.Land))
    val discarded = chooseAnyNumber(
        from = lands,
        prompt = "Discard any number of land cards",
        selectedLabel = "Discard"
    )
    discard(discarded)
    ifNotEmpty(discarded) {
        run(
            Effects.ReflexiveTrigger(
                action = Effects.Nothing,
                optional = false,
                reflexiveEffect = Effects.DealDamage(
                    DynamicAmount.Multiply(discarded.count, 2),
                    EffectTarget.ContextTarget(0)
                ),
                reflexiveTargetRequirements = listOf(TargetObject(filter = TargetFilter.Creature)),
                descriptionOverride = "When you discard one or more cards this way, Borborygmos " +
                    "and Fblthp deals twice that much damage to target creature."
            )
        )
    }
}

/**
 * Borborygmos and Fblthp
 * {2}{G}{U}{R}
 * Legendary Creature — Cyclops Homunculus
 * 6/5
 *
 * Whenever Borborygmos and Fblthp enters or attacks, draw a card, then you may discard any number
 * of land cards. When you discard one or more cards this way, Borborygmos and Fblthp deals twice
 * that much damage to target creature.
 * {1}{U}: Put Borborygmos and Fblthp into its owner's library third from the top.
 *
 * "Enters or attacks" is two triggered abilities sharing one effect. Only land cards are offered
 * for the discard (per the ruling, nonland cards can't be discarded).
 */
val BorborygmosAndFblthp = card("Borborygmos and Fblthp") {
    manaCost = "{2}{G}{U}{R}"
    colorIdentity = "GUR"
    typeLine = "Legendary Creature — Cyclops Homunculus"
    power = 6
    toughness = 5
    oracleText = "Whenever Borborygmos and Fblthp enters or attacks, draw a card, then you may " +
        "discard any number of land cards. When you discard one or more cards this way, " +
        "Borborygmos and Fblthp deals twice that much damage to target creature.\n" +
        "{1}{U}: Put Borborygmos and Fblthp into its owner's library third from the top."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = borborygmosTriggerEffect()
        description = BORBORYGMOS_TRIGGER_TEXT
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = borborygmosTriggerEffect()
        description = BORBORYGMOS_TRIGGER_TEXT
    }

    activatedAbility {
        cost = Costs.Mana("{1}{U}")
        effect = Effects.PutIntoLibraryNthFromTop(EffectTarget.Self, 2)
        description = "{1}{U}: Put Borborygmos and Fblthp into its owner's library third from the top."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "219"
        artist = "Rudy Siswanto"
        imageUri = "https://cards.scryfall.io/normal/front/3/4/34bbf1c1-2868-4c9e-b5c4-1aae86faed6c.jpg?1783916956"
        ruling("2023-04-14", "The first ability of Borborygmos and Fblthp triggers and goes on the stack without a target. If you discard one or more land cards, the second \"reflexive\" triggered ability will trigger. You'll choose the target creature for that second ability at that time.")
        ruling("2023-04-14", "You can't discard nonland cards, even if you want to.")
        ruling("2023-04-14", "If there are one or fewer cards in your library, Borborygmos and Fblthp is put on the bottom of your library.")
    }
}
