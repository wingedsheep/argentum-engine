package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DonorCards
import com.wingedsheep.sdk.scripting.HasAllActivatedAbilitiesOfCards
import com.wingedsheep.sdk.scripting.SpendAnyManaTypeForActivatedAbilities
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Agatha's Soul Cauldron
 * {2}
 * Legendary Artifact (Wilds of Eldraine, mythic)
 *
 * "You may spend mana as though it were mana of any color to activate abilities of creatures
 *  you control.
 *  Creatures you control with +1/+1 counters on them have all activated abilities of all creature
 *  cards exiled with Agatha's Soul Cauldron.
 *  {T}: Exile target card from a graveyard. When a creature card is exiled this way, put a +1/+1
 *  counter on target creature you control."
 *
 * Implementation — three reusable static/activated pieces:
 *  - [SpendAnyManaTypeForActivatedAbilities] scoped to `AllCreaturesYouControl` with
 *    `asThoughAnyColor`: relaxes the colored requirements of the mana cost of any ability activated
 *    from a creature you control (CR 609.4b), so the granted (and printed) abilities of your
 *    creatures can be paid with mana of any color — a `{C}` pip still needs colorless mana. Same
 *    engine seam as Sharkey, Tyrant of the Shire (there scoped to Self, "any type").
 *  - [HasAllActivatedAbilitiesOfCards] with a battlefield `receivedBy` filter ("creatures you control
 *    with a +1/+1 counter") and `cardFilter = Creature`: every creature card in the Cauldron's
 *    linked-exile pile lends its activated abilities to each such creature, with that creature as
 *    the abilities' source (so `{T}` taps it and self-references bind to it — the printed ruling).
 *  - The `{T}` activated ability exiles a targeted graveyard card linked to the Cauldron, then —
 *    only if that card was a creature card (a reflexive trigger, resolved inline via
 *    [Effects.If]) — chooses a creature you control and puts a +1/+1 counter on it.
 *
 * Rulings (2023-09-01):
 *  - Grants only *activated* abilities, never keyword (unless activated), triggered, or static ones.
 *  - The granted abilities use "this permanent," so they're treated as printed on the creature that
 *    gained them.
 */
val AgathasSoulCauldron = card("Agatha's Soul Cauldron") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Legendary Artifact"
    oracleText = "You may spend mana as though it were mana of any color to activate abilities of " +
        "creatures you control.\n" +
        "Creatures you control with +1/+1 counters on them have all activated abilities of all " +
        "creature cards exiled with Agatha's Soul Cauldron.\n" +
        "{T}: Exile target card from a graveyard. When a creature card is exiled this way, put a " +
        "+1/+1 counter on target creature you control."

    // "You may spend mana as though it were mana of any color to activate abilities of creatures
    // you control."
    staticAbility {
        ability = SpendAnyManaTypeForActivatedAbilities(
            filter = GroupFilter.AllCreaturesYouControl,
            asThoughAnyColor = true
        )
    }

    // "Creatures you control with +1/+1 counters on them have all activated abilities of all
    // creature cards exiled with Agatha's Soul Cauldron."
    staticAbility {
        ability = HasAllActivatedAbilitiesOfCards(
            donors = DonorCards.LINKED_EXILE,
            cardFilter = Filters.Creature,
            receivedBy = GroupFilter.AllCreaturesYouControl.withCounter(CounterType.PLUS_ONE_PLUS_ONE)
        )
    }

    // "{T}: Exile target card from a graveyard. When a creature card is exiled this way, put a
    // +1/+1 counter on target creature you control."
    activatedAbility {
        cost = Costs.Tap
        val exiled = target(TargetFilter.CardInGraveyard)
        effect = Effects.ExileLinkedToSource(exiled) then
            // Reflexive "when a creature card is exiled this way": tested on the exiled card's
            // printed type (last-known via its CardComponent in exile). When true, choose a creature
            // you control at resolution and add the counter.
            Effects.If(
                condition = Conditions.TargetIsCreatureCard(0),
                then = Effects.Pipeline {
                    val cauldronCounterTarget = selectTarget(TargetObject(filter = TargetFilter.CreatureYouControl))
                    run(Effects.AddCounters(
                        CounterType.PLUS_ONE_PLUS_ONE,
                        1,
                        cauldronCounterTarget.asTarget
                    ))
                }
            )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "242"
        artist = "Jason A. Engle"
        imageUri = "https://cards.scryfall.io/normal/front/0/1/019b51b0-e5c6-4208-922b-7736686dddcd.jpg?1692939838"
    }
}
