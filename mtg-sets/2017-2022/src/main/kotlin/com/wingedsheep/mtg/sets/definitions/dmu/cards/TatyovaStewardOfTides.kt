package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Tatyova, Steward of Tides
 * {G}{G}{U}
 * Legendary Creature — Merfolk Druid
 * 3/3
 * Land creatures you control have flying.
 * Whenever a land you control enters, if you control seven or more lands, up to one target land
 * you control becomes a 3/3 Elemental creature with haste. It's still a land.
 *
 * - The flying grant is a static over the projected "land creature you control" group (Aang's
 *   shape), so it reaches lands this card or anything else animates.
 * - The landfall trigger is an intervening "if" (Twists and Turns' check), re-checked on
 *   resolution. "Up to one target land you control" may be the land that just entered (ruling).
 *   The animation has no duration on the card, so it is permanent — `BecomeCreature` keeps the
 *   land's types and abilities ("It's still a land").
 */
val TatyovaStewardOfTides = card("Tatyova, Steward of Tides") {
    manaCost = "{G}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Legendary Creature — Merfolk Druid"
    power = 3
    toughness = 3
    oracleText = "Land creatures you control have flying.\n" +
        "Whenever a land you control enters, if you control seven or more lands, up to one target land " +
        "you control becomes a 3/3 Elemental creature with haste. It's still a land."

    staticAbility {
        ability = GrantKeyword(
            keyword = Keyword.FLYING,
            filter = GroupFilter((GameObjectFilter.Creature and GameObjectFilter.Land).youControl())
        )
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Land.youControl()).enters()
        interveningIf = Conditions.YouControlAtLeast(7, GameObjectFilter.Land)
        val land = target(TargetFilter(GameObjectFilter.Land.youControl()), optional = true)
        effect = Effects.BecomeCreature(
            target = land,
            power = 3,
            toughness = 3,
            keywords = setOf(Keyword.HASTE),
            creatureTypes = setOf("Elemental"),
            duration = Duration.Permanent
        )
        description = "Whenever a land you control enters, if you control seven or more lands, up to one " +
            "target land you control becomes a 3/3 Elemental creature with haste. It's still a land."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "222"
        artist = "Howard Lyon"
        imageUri = "https://cards.scryfall.io/normal/front/0/d/0d446cb9-519a-4889-b057-029db9400021.jpg?1783921276"

        ruling(
            "2022-09-09",
            "You may choose to target the land that entered the battlefield with Tatyova, Steward of Tides's triggered ability."
        )
        ruling(
            "2022-09-09",
            "If multiple lands enter the battlefield simultaneously, all of those lands are counted. For example, if you control six lands and two lands enter the battlefield at the same time, Tatyova's ability will trigger twice."
        )
    }
}
