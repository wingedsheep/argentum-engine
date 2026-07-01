package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Wan Shi Tong, Librarian
 * {X}{U}{U}
 * Legendary Creature — Bird Spirit
 * 1/1
 *
 * Flash
 * Flying, vigilance
 * When Wan Shi Tong enters, put X +1/+1 counters on him. Then draw half X cards, rounded down.
 * Whenever an opponent searches their library, put a +1/+1 counter on Wan Shi Tong and draw a card.
 *
 * The {X} cast prompts for X, which flows to the ETB trigger via [DynamicAmount.XValue] (same paid
 * value drives both clauses): X +1/+1 counters via [Effects.AddDynamicCounters], then
 * `floor(X / 2)` cards via [Effects.DrawCards] over [DynamicAmount.Divide] with `roundUp = false`
 * (integer division = round down).
 *
 * The opponent-search clause is the engine's new
 * [Triggers.WheneverAnOpponentSearchesTheirLibrary] (CR 701.23) — every tutor / fetch / basic-land
 * search an opponent resolves fires the auto-emitted `LibrarySearchedEvent`; since searching is the
 * act of looking (CR 701.23a) and finding a card is not required (CR 701.23b), it fires even when
 * the opponent finds nothing. The controller's own searches are not opponents', so they never
 * trigger it.
 */
val WanShiTongLibrarian = card("Wan Shi Tong, Librarian") {
    manaCost = "{X}{U}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Bird Spirit"
    power = 1
    toughness = 1
    oracleText = "Flash\n" +
        "Flying, vigilance\n" +
        "When Wan Shi Tong enters, put X +1/+1 counters on him. Then draw half X cards, rounded down.\n" +
        "Whenever an opponent searches their library, put a +1/+1 counter on Wan Shi Tong and draw a card."

    keywords(Keyword.FLASH, Keyword.FLYING, Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.Composite(
            Effects.AddDynamicCounters(Counters.PLUS_ONE_PLUS_ONE, DynamicAmount.XValue, EffectTarget.Self),
            Effects.DrawCards(
                DynamicAmount.Divide(DynamicAmount.XValue, DynamicAmount.Fixed(2), roundUp = false),
            ),
        )
        description = "Put X +1/+1 counters on Wan Shi Tong. Then draw half X cards, rounded down."
    }

    triggeredAbility {
        trigger = Triggers.WheneverAnOpponentSearchesTheirLibrary
        effect = Effects.Composite(
            Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self),
            Effects.DrawCards(1),
        )
        description = "Put a +1/+1 counter on Wan Shi Tong and draw a card."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "78"
        artist = "Ryota Murayama"
        imageUri = "https://cards.scryfall.io/normal/front/e/2/e20da6b5-1057-4a28-9e85-07de714e262f.jpg?1764120524"
    }
}
