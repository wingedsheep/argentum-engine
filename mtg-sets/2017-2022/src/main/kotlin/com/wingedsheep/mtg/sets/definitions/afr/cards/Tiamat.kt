package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.EmitLibrarySearchedEventEffect
import com.wingedsheep.sdk.scripting.effects.SelectionRestriction
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Tiamat — Adventures in the Forgotten Realms #235
 * {2}{W}{U}{B}{R}{G} · Legendary Creature — Dragon God · 7/7 · Mythic
 *
 * Flying
 * When Tiamat enters, if you cast it, search your library for up to five Dragon cards not named
 * Tiamat that each have different names, reveal them, put them into your hand, then shuffle.
 *
 * "If you cast it" is the [Conditions.WasCast] intervening-if (CR 603.4) — any zone counts, per the
 * ruling. The search is the Three Dreams pipeline: gather library Dragons `notNamed("Tiamat")` →
 * `chooseUpTo(5)` under [SelectionRestriction.OnePerCardName] → reveal into hand → shuffle.
 */
val Tiamat = card("Tiamat") {
    manaCost = "{2}{W}{U}{B}{R}{G}"
    colorIdentity = "WUBRG"
    typeLine = "Legendary Creature — Dragon God"
    power = 7
    toughness = 7
    oracleText = "Flying\n" +
        "When Tiamat enters, if you cast it, search your library for up to five Dragon cards not " +
        "named Tiamat that each have different names, reveal them, put them into your hand, then shuffle."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.WasCast
        effect = Effects.Pipeline {
            val searchable = gather(
                CardSource.FromZone(
                    Zone.LIBRARY,
                    Player.You,
                    GameObjectFilter.Any.withSubtype("Dragon").notNamed("Tiamat")
                ),
                search = true
            )
            val found = chooseUpTo(
                5,
                from = searchable,
                restrictions = listOf(SelectionRestriction.OnePerCardName),
                prompt = "Search for up to five Dragon cards not named Tiamat with different names"
            )
            toHand(found, revealed = true)
            run(Effects.ShuffleLibrary())
            // CR 701.23 — the search happened whether or not anything was found.
            run(EmitLibrarySearchedEventEffect)
        }
        description = "When Tiamat enters, if you cast it, search your library for up to five Dragon " +
            "cards not named Tiamat that each have different names, reveal them, put them into your " +
            "hand, then shuffle."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "235"
        artist = "Chris Rahn"
        flavorText = "\"My children will rule over all!\""
        imageUri = "https://cards.scryfall.io/normal/front/6/d/6dd0b9b0-55f4-4ce7-a916-6f23687f3fe4.jpg?1783926443"
        ruling(
            "2021-07-23",
            "Tiamat's enters-the-battlefield ability will trigger if you cast Tiamat from any zone, not " +
                "just from your hand. Still thinking command zone over here."
        )
        ruling(
            "2021-07-23",
            "A Dragon card is a card with the creature type Dragon in its type line. A card that has " +
                "\"Dragon\" in the name (such as Dragon's Disciple) isn't a Dragon card unless it also has " +
                "the Dragon creature type."
        )
    }
}
