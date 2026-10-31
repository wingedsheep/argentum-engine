package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.ManaColorSet

/**
 * Narset of the Ancient Way — Ikoria: Lair of Behemoths #195
 * {1}{U}{R}{W} · Legendary Planeswalker — Narset · Starting loyalty 4
 *
 * +1: You gain 2 life. Add {U}, {R}, or {W}. Spend this mana only to cast a noncreature spell.
 * −2: Draw a card, then you may discard a card. When you discard a nonland card this way, Narset
 *     deals damage equal to that card's mana value to target creature or planeswalker.
 * −6: You get an emblem with "Whenever you cast a noncreature spell, this emblem deals 2 damage
 *     to any target."
 *
 * Modeling notes:
 *
 *  - **The +1** is a loyalty ability, not a mana ability (ruling) — it uses the stack. The mana is
 *    the negated creature-type restriction (The Emperor of Palamecia's "noncreature spell" spelling).
 *  - **The −2** lets you discard *any* card (a land included), but only a nonland discard arms the
 *    CR 603.12 reflexive trigger — so the discard is an up-to-one selection from hand, and the
 *    reflexive trigger is gated on the discarded collection holding a nonland card (the Hidetsugu,
 *    Devouring Chaos shape). The target is chosen only when the reflexive ability triggers, after
 *    the drawn card is seen (ruling). X in a discarded card's cost counts as 0 via mana value.
 *  - **The emblem** is a global triggered ability with `emblem = true`, so the colorless emblem is
 *    the damage source and it outlives Narset (Chandra, Torch of Defiance idiom).
 */
val NarsetOfTheAncientWay = card("Narset of the Ancient Way") {
    manaCost = "{1}{U}{R}{W}"
    colorIdentity = "URW"
    typeLine = "Legendary Planeswalker — Narset"
    startingLoyalty = 4
    oracleText = "+1: You gain 2 life. Add {U}, {R}, or {W}. Spend this mana only to cast a " +
        "noncreature spell.\n" +
        "−2: Draw a card, then you may discard a card. When you discard a nonland card this way, " +
        "Narset deals damage equal to that card's mana value to target creature or planeswalker.\n" +
        "−6: You get an emblem with \"Whenever you cast a noncreature spell, this emblem deals 2 " +
        "damage to any target.\""

    // +1: You gain 2 life. Add {U}, {R}, or {W}. Spend this mana only to cast a noncreature spell.
    loyaltyAbility(+1) {
        effect = Effects.GainLife(2) then Effects.AddManaOfChoice(
            colorSet = ManaColorSet.Specific(setOf(Color.BLUE, Color.RED, Color.WHITE)),
            restriction = ManaRestriction.CardTypeSpellsOrAbilitiesOnly(
                cardType = CardType.CREATURE,
                negated = true,
            ),
        )
        description = "You gain 2 life. Add {U}, {R}, or {W}. Spend this mana only to cast a " +
            "noncreature spell."
    }

    // −2: Draw a card, then you may discard a card. When you discard a nonland card this way,
    //     Narset deals damage equal to that card's mana value to target creature or planeswalker.
    loyaltyAbility(-2) {
        effect = Effects.Pipeline {
            run(Effects.DrawCards(1))
            val hand = gather(CardSource.FromZone(Zone.HAND, Player.You))
            val discarded = chooseUpTo(1, from = hand, prompt = "You may discard a card")
            discard(discarded)
            run(
                Effects.If(
                    condition = Conditions.CollectionContainsMatch(discarded, GameObjectFilter.Nonland),
                    then = Effects.ReflexiveTrigger(
                        action = Effects.Nothing,
                        optional = false,
                        descriptionOverride = "Narset deals damage equal to the discarded card's " +
                            "mana value to target creature or planeswalker.",
                    ) {
                        val creatureOrPlaneswalker = target(Targets.CreatureOrPlaneswalker)
                        effect = Effects.DealDamage(DynamicAmounts.manaValueOf(discarded), creatureOrPlaneswalker)
                    },
                )
            )
        }
        description = "Draw a card, then you may discard a card. When you discard a nonland card " +
            "this way, Narset deals damage equal to that card's mana value to target creature or " +
            "planeswalker."
    }

    // −6: Emblem — "Whenever you cast a noncreature spell, this emblem deals 2 damage to any target."
    loyaltyAbility(-6) {
        effect = Effects.CreateGlobalTriggeredAbility(
            ability = grantedTriggeredAbility {
                trigger = Triggers.you.casts(GameObjectFilter.Noncreature)
                val anyTarget = target(Targets.Any)
                effect = Effects.DealDamage(2, anyTarget)
                description = "Whenever you cast a noncreature spell, this emblem deals 2 damage to " +
                    "any target."
            },
            emblem = true,
            descriptionOverride = "Whenever you cast a noncreature spell, this emblem deals 2 damage " +
                "to any target."
        )
        description = "You get an emblem with \"Whenever you cast a noncreature spell, this emblem " +
            "deals 2 damage to any target.\""
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "195"
        artist = "Yongjae Choi"
        imageUri = "https://cards.scryfall.io/normal/front/f/a/fa7b28d8-b835-44a0-978d-cadfd392fff5.jpg?1783931021"

        ruling("2020-04-17", "Because it's a loyalty ability, Narset's first ability isn't a mana ability. It can be activated only any time you could cast a sorcery. It uses the stack and can be responded to.")
        ruling("2020-04-17", "Narset's second ability goes on the stack without a target. While that ability is resolving, you draw a card and may choose to discard a card. When you discard a nonland card this way, the reflexive triggered ability triggers and you pick a target to be dealt damage. This is different from effects that say \"If you do . . .\" in that you choose a target after looking at the card you draw and players can respond to the reflexive triggered ability knowing how much damage will be dealt.")
        ruling("2020-04-17", "You can discard a card even if Narset leaves the battlefield before her middle ability resolves. The reflexive triggered ability will still trigger if you discard a nonland card.")
        ruling("2020-04-17", "If a card you discard has {X} in its mana cost, X is considered to be 0.")
        ruling("2020-04-17", "The emblem created by Narset's last ability is colorless. The damage it deals is from a colorless source.")
    }
}
