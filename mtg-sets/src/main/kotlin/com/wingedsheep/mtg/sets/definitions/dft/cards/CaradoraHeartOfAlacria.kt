package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyCounterPlacement
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.events.RecipientFilter

/**
 * Caradora, Heart of Alacria — Aetherdrift #195
 * {2}{G}{W} · Legendary Creature — Human Knight · 4/2
 *
 * When Caradora enters, you may search your library for a Mount or Vehicle card, reveal it, put
 * it into your hand, then shuffle.
 * If one or more +1/+1 counters would be put on a creature or Vehicle you control, that many plus
 * one +1/+1 counters are put on it instead.
 *
 * The tutor is [Patterns.Library.searchLibrary] with `reveal = true`: the pattern selects with
 * `ChooseUpTo(1)`, so choosing nothing *is* the declined "you may" — no separate optional gate,
 * and a shuffle still happens either way (CR 701.19c).
 *
 * The counter clause is Hardened Scales' [ModifyCounterPlacement] with a widened recipient. The
 * default `appliesTo` is "a creature you control"; Caradora also covers uncrewed Vehicles, which
 * aren't creatures, so the recipient becomes a [RecipientFilter.Matching] over the
 * creature-or-Vehicle union. Modelling it as the counter-placement *replacement* (rather than a
 * trigger that adds one more counter) is what makes the printed rulings fall out for free:
 * a permanent entering with +1/+1 counters enters with one extra, two Caradoras stack, and the
 * controller orders it against other placement replacements per CR 616.1.
 */
private val MountOrVehicleCard = GameObjectFilter.Any
    .withAnyOfSubtypes(listOf(Subtype("Mount"), Subtype.VEHICLE))

private val CreatureOrVehicleYouControl =
    (GameObjectFilter.Creature or GameObjectFilter.Permanent.withSubtype(Subtype.VEHICLE))
        .youControl()

val CaradoraHeartOfAlacria = card("Caradora, Heart of Alacria") {
    manaCost = "{2}{G}{W}"
    colorIdentity = "GW"
    typeLine = "Legendary Creature — Human Knight"
    power = 4
    toughness = 2
    oracleText = "When Caradora enters, you may search your library for a Mount or Vehicle card, " +
        "reveal it, put it into your hand, then shuffle.\n" +
        "If one or more +1/+1 counters would be put on a creature or Vehicle you control, that " +
        "many plus one +1/+1 counters are put on it instead."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Patterns.Library.searchLibrary(
            filter = MountOrVehicleCard,
            destination = SearchDestination.HAND,
            reveal = true
        )
        description = "When Caradora enters, you may search your library for a Mount or Vehicle " +
            "card, reveal it, put it into your hand, then shuffle."
    }

    replacementEffect(
        ModifyCounterPlacement(
            modifier = 1,
            appliesTo = EventPattern.CounterPlacementEvent(
                counterType = CounterTypeFilter.PlusOnePlusOne,
                recipient = RecipientFilter.Matching(CreatureOrVehicleYouControl)
            )
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "195"
        artist = "Mirko Failoni"
        imageUri = "https://cards.scryfall.io/normal/front/1/2/1256d22d-a2a9-41fb-b669-0661ba230bc7.jpg?1783907860"
        ruling(
            "2025-02-07",
            "If a creature or Vehicle you control would enter the battlefield with a number of " +
                "+1/+1 counters on it, it enters with that many plus one instead."
        )
        ruling(
            "2025-02-07",
            "If you somehow control multiple copies of Caradora, they would each increase the " +
                "number of +1/+1 counters put on a creature or Vehicle you control by one."
        )
    }
}
