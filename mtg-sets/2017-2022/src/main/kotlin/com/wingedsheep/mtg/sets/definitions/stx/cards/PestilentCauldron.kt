package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Pestilent Cauldron // Restorative Burst — Strixhaven: School of Mages #154 (canonical printing)
 * {2}{B} Artifact // {3}{G}{G} Sorcery
 * Modal double-faced card (CR 712): cast either face.
 *
 * Front — Pestilent Cauldron:
 *  - "{T}, Discard a card: Create a Pest." Tap + discard composite cost, the set's predefined
 *    Pest via [Effects.CreatePest].
 *  - "{1}, {T}: Each opponent mills cards equal to the amount of life you gained this turn."
 *    [Patterns.Library.mill] over each opponent, counted by [DynamicAmounts.lifeGainedThisTurn] —
 *    gross life gained, not net of life lost (ruling).
 *  - "{4}, {T}: Exile four target cards from a single graveyard. Draw a card." Rag Dealer's
 *    same-owner graveyard targeting, but exactly four (not "up to") so the ability can't be
 *    activated without four legal targets in one graveyard (ruling).
 *
 * Back — Restorative Burst: up to two target creature/land/planeswalker cards from your
 * graveyard return to hand, each player gains 4 life, and the spell exiles itself. Zero targets
 * is legal; if every chosen target is illegal the spell fizzles whole (ruling).
 */
private val restorableCardInYourGraveyard = TargetFilter(
    (GameObjectFilter.Creature or GameObjectFilter.Land or GameObjectFilter.Planeswalker).ownedByYou(),
    zone = Zone.GRAVEYARD,
)

val PestilentCauldron = card("Pestilent Cauldron") {
    manaCost = "{2}{B}"
    colorIdentity = "BG"
    typeLine = "Artifact"
    oracleText = "{T}, Discard a card: Create a 1/1 black and green Pest creature token with " +
        "\"When this token dies, you gain 1 life.\"\n" +
        "{1}, {T}: Each opponent mills cards equal to the amount of life you gained this turn.\n" +
        "{4}, {T}: Exile four target cards from a single graveyard. Draw a card."

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.DiscardCard)
        effect = Effects.CreatePest()
        description = "Create a 1/1 black and green Pest creature token with \"When this token " +
            "dies, you gain 1 life.\""
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap)
        effect = Patterns.Library.mill(
            DynamicAmounts.lifeGainedThisTurn(),
            EffectTarget.PlayerRef(Player.EachOpponent),
        )
        description = "Each opponent mills cards equal to the amount of life you gained this turn."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{4}"), Costs.Tap)
        targets(TargetFilter.CardInGraveyard, count = 4, sameOwner = true)
        effect = Effects.ForEachTarget(Effects.Move(EffectTarget.ContextTarget(0), Zone.EXILE)) then
            Effects.DrawCards(1)
        description = "Exile four target cards from a single graveyard. Draw a card."
    }

    modalBack("Restorative Burst") {
        manaCost = "{3}{G}{G}"
        typeLine = "Sorcery"
        oracleText = "Return up to two target creature, land, and/or planeswalker cards from your " +
            "graveyard to your hand. Each player gains 4 life. Exile Restorative Burst."
        imageUri = "https://cards.scryfall.io/normal/back/a/a/aaa1e6be-08cc-4ccc-b2de-3511613e4fd0.jpg?1783927335"
        spell {
            selfExile()
            targets(restorableCardInYourGraveyard, count = 2, optional = true)
            effect = Effects.ForEachTarget(Effects.Move(EffectTarget.ContextTarget(0), Zone.HAND)) then
                Effects.GainLife(4, EffectTarget.PlayerRef(Player.Each))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "154"
        artist = "Dan Murayama Scott"
        imageUri = "https://cards.scryfall.io/normal/front/a/a/aaa1e6be-08cc-4ccc-b2de-3511613e4fd0.jpg?1783927335"
        ruling("2021-04-16", "Pestilent Cauldron's second ability counts the total amount of life gained without considering any life you lost during that turn. For example, if you lost 2 life and gained 2 life earlier in the turn, your opponents will mill two cards.")
        ruling("2021-04-16", "You must be able to target four cards in a single graveyard in order to activate Pestilent Cauldron's last ability.")
        ruling("2021-04-16", "You may cast Restorative Burst with no targets. If you do, each player will gain 4 life and you'll exile Restorative Burst. However, if you choose one or two target cards, and each of those cards is an illegal target as Restorative Burst tries to resolve (usually because something else moved them in response), Restorative Burst won't resolve and none of its effects will happen. No one will gain life, and Restorative Burst won't be exiled.")
    }
}
