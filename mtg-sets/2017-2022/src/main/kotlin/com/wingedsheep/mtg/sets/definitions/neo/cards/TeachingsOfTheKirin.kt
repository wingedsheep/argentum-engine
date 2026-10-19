package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Teachings of the Kirin // Kirin-Touched Orochi
 * {1}{G} — Enchantment — Saga // Enchantment Creature — Snake Monk 1/1
 *
 * Front — Teachings of the Kirin:
 *   I — Mill three cards. Create a 1/1 colorless Spirit creature token.
 *   II — Put a +1/+1 counter on target creature you control.
 *   III — Exile this Saga, then return it to the battlefield transformed under your control.
 *
 * Back — Kirin-Touched Orochi:
 *   Whenever this creature attacks, choose one —
 *   • Exile target creature card from a graveyard. When you do, create a 1/1 colorless Spirit
 *     creature token.
 *   • Exile target noncreature card from a graveyard. When you do, put a +1/+1 counter on target
 *     creature you control.
 *
 * Each mode's "when you do" is a real reflexive trigger (CR 603.12): the exile is the mode's action,
 * and the payoff goes on the stack afterwards — so the second mode's creature is chosen only once
 * the noncreature card is gone. If the graveyard target is illegal on resolution, the ability does
 * nothing and the reflexive trigger never fires.
 */
private fun spiritToken() = Effects.CreateToken(
    power = 1,
    toughness = 1,
    creatureTypes = setOf("Spirit"),
    imageUri = "https://cards.scryfall.io/normal/front/c/a/ca20548f-6324-4858-adbe-87303ff1ca52.jpg?1783923715",
)

private val KirinTouchedOrochi = card("Kirin-Touched Orochi") {
    manaCost = ""
    colorIndicator = "G"
    colorIdentity = "G"
    typeLine = "Enchantment Creature — Snake Monk"
    oracleText = "Whenever this creature attacks, choose one —\n" +
        "• Exile target creature card from a graveyard. When you do, create a 1/1 colorless Spirit " +
        "creature token.\n" +
        "• Exile target noncreature card from a graveyard. When you do, put a +1/+1 counter on " +
        "target creature you control."
    power = 1
    toughness = 1

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = ModalEffect.chooseOne(
            mode(
                "Exile target creature card from a graveyard. When you do, create a 1/1 colorless " +
                    "Spirit creature token."
            ) {
                val creatureCard = target(TargetFilter(GameObjectFilter.Creature, zone = Zone.GRAVEYARD))
                effect = Effects.ReflexiveTrigger(
                    action = Effects.Exile(creatureCard),
                    reflexiveEffect = spiritToken(),
                    optional = false,
                    descriptionOverride = "Exile target creature card from a graveyard. When you do, " +
                        "create a 1/1 colorless Spirit creature token.",
                )
            },
            mode(
                "Exile target noncreature card from a graveyard. When you do, put a +1/+1 counter on " +
                    "target creature you control."
            ) {
                val noncreatureCard = target(TargetFilter(GameObjectFilter.Noncreature, zone = Zone.GRAVEYARD))
                effect = Effects.ReflexiveTrigger(
                    action = Effects.Exile(noncreatureCard),
                    optional = false,
                    descriptionOverride = "Exile target noncreature card from a graveyard. When you do, " +
                        "put a +1/+1 counter on target creature you control.",
                ) {
                    val creature = target(TargetFilter.Creature.youControl())
                    effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature)
                }
            },
        )
        description = "Whenever this creature attacks, choose one — • Exile target creature card from " +
            "a graveyard. When you do, create a 1/1 colorless Spirit creature token. • Exile target " +
            "noncreature card from a graveyard. When you do, put a +1/+1 counter on target creature " +
            "you control."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "212"
        artist = "Sam Burley"
        imageUri = "https://cards.scryfall.io/normal/back/4/c/4c0c6c2a-4352-4c65-b41d-2a722bbef6c5.jpg?1783923843"
    }
}

private val TeachingsOfTheKirinFront = card("Teachings of the Kirin") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter.)\n" +
        "I — Mill three cards. Create a 1/1 colorless Spirit creature token.\n" +
        "II — Put a +1/+1 counter on target creature you control.\n" +
        "III — Exile this Saga, then return it to the battlefield transformed under your control."

    sagaChapter(1) {
        effect = Patterns.Library.mill(3) then spiritToken()
    }

    sagaChapter(2) {
        val creature = target(TargetFilter.Creature.youControl())
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature)
    }

    sagaChapter(3) {
        effect = Effects.ExileAndReturnTransformed()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "212"
        artist = "Sam Burley"
        imageUri = "https://cards.scryfall.io/normal/front/4/c/4c0c6c2a-4352-4c65-b41d-2a722bbef6c5.jpg?1783923843"
    }
}

val TeachingsOfTheKirin: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = TeachingsOfTheKirinFront,
    backFace = KirinTouchedOrochi,
)
