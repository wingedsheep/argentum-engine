package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.daybound
import com.wingedsheep.sdk.dsl.nightbound
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Howlpack Piper // Wildsong Howler (Innistrad: Crimson Vow)
 * {3}{G}
 * Creature — Human Werewolf // Creature — Werewolf
 *
 * Front — Howlpack Piper (2/2): "This spell can't be countered"; "{1}{G}, {T}: You may put a creature
 *          card from your hand onto the battlefield. If it's a Wolf or Werewolf, untap this creature.
 *          Activate only as a sorcery"; Daybound.
 * Back  — Wildsong Howler (4/4): "Whenever this creature enters or transforms into Wildsong Howler, look
 *          at the top six cards of your library. You may reveal a creature card from among them and put
 *          it into your hand. Put the rest on the bottom of your library in a random order"; Nightbound.
 *
 * The front's "can't be countered" is the `cantBeCountered` spell flag (CR 701.5). Its activated ability
 * is the Cultivator Colossus [Patterns.Hand.putFromHand] rail — `ChooseUpTo(1)` makes "you **may** put"
 * a legal decline. It's sorcery-speed (`timing = TimingRule.SorcerySpeed`) and taps as a cost
 * ([Costs.Composite] of `{1}{G}` and [Costs.Tap]). The untap rider fires only when the put creature is a
 * Wolf or Werewolf: a [ConditionalEffect] gated on [Conditions.CollectionContainsMatch] over the
 * pipeline's `putting` collection (the same collection Cultivator Colossus's loop reads), so declining or
 * putting a non-Wolf leaves the Piper tapped.
 *
 * The back's payoff triggers on **enters or transforms into Wildsong Howler** — two triggers, an
 * [Triggers.EntersBattlefield] and a [Triggers.TransformsToBack] — each running the Radagast
 * [Patterns.Library.lookAtTopRevealMatchingToHand] over the top six cards, keeping up to one creature
 * card and bottoming the rest in a random order.
 *
 * The back is a transformed face with no mana cost, so its color comes from a color indicator (CR 204):
 * `colorIndicator = "G"`.
 */

private val WILDSONG_HOWLER_DIG = Patterns.Library.lookAtTopRevealMatchingToHand(
    count = DynamicAmount.Fixed(6),
    filter = GameObjectFilter.Creature,
    prompt = "You may reveal a creature card to put into your hand",
)

private val HowlpackPiperFront = card("Howlpack Piper") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Werewolf"
    power = 2
    toughness = 2
    cantBeCountered = true
    oracleText = "This spell can't be countered.\n" +
        "{1}{G}, {T}: You may put a creature card from your hand onto the battlefield. If it's a Wolf " +
        "or Werewolf, untap this creature. Activate only as a sorcery.\n" +
        "Daybound (If a player casts no spells during their own turn, it becomes night next turn.)"

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{G}"), Costs.Tap)
        timing = TimingRule.SorcerySpeed
        effect = Effects.Composite(
            Patterns.Hand.putFromHand(filter = GameObjectFilter.Creature, count = 1),
            ConditionalEffect(
                condition = Conditions.CollectionContainsMatch(
                    "putting",
                    GameObjectFilter.Creature.withAnySubtype("Wolf", "Werewolf"),
                ),
                effect = Effects.Untap(EffectTarget.Self),
            ),
        )
        description = "You may put a creature card from your hand onto the battlefield. If it's a Wolf " +
            "or Werewolf, untap this creature."
    }
    daybound()

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "205"
        artist = "Cristi Balanescu"
        imageUri = "https://cards.scryfall.io/normal/front/c/7/c7ceaf83-09c0-4492-a75d-4c47bd421858.jpg?1783924816"
    }
}

private val WildsongHowler = card("Wildsong Howler") {
    manaCost = ""
    colorIdentity = "G"
    colorIndicator = "G" // Transformed back face, no mana cost (CR 204).
    typeLine = "Creature — Werewolf"
    power = 4
    toughness = 4
    oracleText = "Whenever this creature enters or transforms into Wildsong Howler, look at the top six " +
        "cards of your library. You may reveal a creature card from among them and put it into your " +
        "hand. Put the rest on the bottom of your library in a random order.\n" +
        "Nightbound (If a player casts at least two spells during their own turn, it becomes day next turn.)"

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = WILDSONG_HOWLER_DIG
        description = "Look at the top six cards of your library. You may reveal a creature card from " +
            "among them and put it into your hand. Put the rest on the bottom of your library in a " +
            "random order."
    }
    triggeredAbility {
        trigger = Triggers.TransformsToBack
        effect = WILDSONG_HOWLER_DIG
        description = "Look at the top six cards of your library. You may reveal a creature card from " +
            "among them and put it into your hand. Put the rest on the bottom of your library in a " +
            "random order."
    }
    nightbound()

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "205"
        artist = "Cristi Balanescu"
        imageUri = "https://cards.scryfall.io/normal/back/c/7/c7ceaf83-09c0-4492-a75d-4c47bd421858.jpg?1783924816"
    }
}

val HowlpackPiper: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = HowlpackPiperFront,
    backFace = WildsongHowler,
)
