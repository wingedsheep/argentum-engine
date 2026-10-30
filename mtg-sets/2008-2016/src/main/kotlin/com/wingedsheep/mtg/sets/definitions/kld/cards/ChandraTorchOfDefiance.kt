package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.SuccessCriterion
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Chandra, Torch of Defiance — Kaladesh #110
 * {2}{R}{R} · Legendary Planeswalker — Chandra · Starting loyalty 4
 *
 * +1: Exile the top card of your library. You may cast that card. If you don't, Chandra deals 2
 *     damage to each opponent.
 * +1: Add {R}{R}.
 * −3: Chandra deals 4 damage to target creature.
 * −7: You get an emblem with "Whenever you cast a spell, this emblem deals 5 damage to any target."
 *
 * Modeling notes:
 *
 *  - **The first +1** casts *during resolution* (ruling: "You can't wait to cast it later in the
 *    turn"), paying its costs — [Effects.CastFromCollection], not a may-play grant. Only a nonland
 *    card is offered (a land can't be *cast*, so a land flip always deals the 2 damage). The "If
 *    you don't" branch is an [Effects.IfYouDo] keyed on the collection the cast publishes to, so
 *    declining the "may", flipping a land, being unable to pay, or having no legal targets for
 *    the spell all fall through to the damage.
 *  - **The second +1** is a loyalty ability, not a mana ability (ruling) — it uses the stack.
 *  - **The emblem** is a global triggered ability ([Effects.CreateGlobalTriggeredAbility]), so it
 *    outlives Chandra, and it triggers (and resolves) above the spell that caused it.
 */
val ChandraTorchOfDefiance = card("Chandra, Torch of Defiance") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Planeswalker — Chandra"
    startingLoyalty = 4
    oracleText = "+1: Exile the top card of your library. You may cast that card. If you don't, " +
        "Chandra deals 2 damage to each opponent.\n" +
        "+1: Add {R}{R}.\n" +
        "−3: Chandra deals 4 damage to target creature.\n" +
        "−7: You get an emblem with \"Whenever you cast a spell, this emblem deals 5 damage to " +
        "any target.\""

    // +1: Exile the top card of your library. You may cast that card. If you don't, 2 to each opponent.
    loyaltyAbility(+1) {
        effect = Effects.IfYouDo(
            action = Effects.Pipeline {
                val exiled = gather(CardSource.TopOfLibrary(DynamicAmounts.fixed(1)))
                exile(exiled)
                // "Cast" never covers playing a land (CR 305.1 via the ruling).
                val castable = filter(exiled, GameObjectFilter.Nonland)
                ifNotEmpty(castable) {
                    run(Effects.May(
                        Effects.CastFromCollection(castable, storeCastTo = "chandraCast"),
                        descriptionOverride = "You may cast that card."
                    ))
                }
            },
            then = Effects.Nothing,
            otherwise = Effects.DealDamage(2, EffectTarget.PlayerRef(Player.EachOpponent)),
            successCriterion = SuccessCriterion.CollectionNonEmpty("chandraCast"),
        )
        description = "Exile the top card of your library. You may cast that card. If you don't, " +
            "Chandra deals 2 damage to each opponent."
    }

    // +1: Add {R}{R}.
    loyaltyAbility(+1) {
        effect = Effects.AddMana(Color.RED, 2)
        description = "Add {R}{R}."
    }

    // −3: Chandra deals 4 damage to target creature.
    loyaltyAbility(-3) {
        val creature = target(TargetFilter.Creature)
        effect = Effects.DealDamage(4, creature)
    }

    // −7: Emblem — "Whenever you cast a spell, this emblem deals 5 damage to any target."
    loyaltyAbility(-7) {
        effect = Effects.CreateGlobalTriggeredAbility(
            ability = grantedTriggeredAbility {
                trigger = Triggers.you.casts()
                val anyTarget = target(Targets.Any)
                effect = Effects.DealDamage(5, anyTarget)
                description = "Whenever you cast a spell, this emblem deals 5 damage to any target."
            },
            emblem = true,
            descriptionOverride = "Whenever you cast a spell, this emblem deals 5 damage to any target."
        )
        description = "You get an emblem with \"Whenever you cast a spell, this emblem deals 5 " +
            "damage to any target.\""
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "110"
        artist = "Magali Villeneuve"
        imageUri = "https://cards.scryfall.io/normal/front/f/f/ff8086cd-b868-4f4e-823e-2635ad7ebc07.jpg?1783937196"

        ruling("2016-09-20", "An effect that instructs you to \"cast\" a card doesn't allow you to play lands. If the card exiled with Chandra's first ability is a land card, you can't play it and Chandra deals 2 damage to each opponent.")
        ruling("2016-09-20", "If you cast the exiled card, you do so as part of the resolution of Chandra's ability. You can't wait to cast it later in the turn. Timing permissions based on the card's type are ignored, but other restrictions (such as \"Cast [this card] only during combat\") are not.")
        ruling("2016-09-20", "You pay the costs for the exiled card if you cast it. You may pay alternative costs such as emerge rather than the card's mana cost.")
        ruling("2016-09-20", "Loyalty abilities can't be mana abilities. Chandra's second ability uses the stack and can be countered or otherwise responded to. Like all loyalty abilities, it can be activated only once per turn, during your main phase, when the stack is empty, and only if no other loyalty abilities of the planeswalker have been activated this turn.")
        ruling("2016-09-20", "The emblem created by Chandra's last ability is colorless. The damage it deals is from a colorless source.")
        ruling("2016-09-20", "Chandra's emblem's ability resolves before the spell that caused it to trigger.")
        ruling("2016-09-20", "In a Two-Headed Giant game, Chandra's first ability causes 4 damage total to be dealt to the opposing team.")
    }
}
