package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.OptionType
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Pixie Illusionist
 * {U}
 * Creature — Faerie Wizard
 * 1/1
 * Kicker {3}{G}
 * Flying
 * If this creature was kicked, it enters with two +1/+1 counters on it.
 * {T}: Target land you control becomes the basic land type of your choice until end of turn.
 *
 * - "Enters with counters" is a replacement effect (CR 614.1c), not a trigger — there is no window
 *   in which a kicked Illusionist is on the battlefield without its counters.
 * - "Becomes" replaces the land's existing land subtypes (CR 305.7) — [Effects.SetLandType], not the
 *   additive [Effects.AddSubtype]. The basic land type is chosen on resolution (ruling), so the
 *   choice lives inside the ability's pipeline, after the target is locked in.
 */
val PixieIllusionist = card("Pixie Illusionist") {
    manaCost = "{U}"
    colorIdentity = "UG"
    typeLine = "Creature — Faerie Wizard"
    power = 1
    toughness = 1
    oracleText = "Kicker {3}{G} (You may pay an additional {3}{G} as you cast this spell.)\n" +
        "Flying\n" +
        "If this creature was kicked, it enters with two +1/+1 counters on it.\n" +
        "{T}: Target land you control becomes the basic land type of your choice until end of turn."

    keywordAbility(KeywordAbility.kicker("{3}{G}"))
    keywords(Keyword.FLYING)

    replacementEffect(EntersWithCounters(
        counterType = CounterType.PLUS_ONE_PLUS_ONE,
        count = 2,
        selfOnly = true,
        condition = Conditions.WasKicked,
    ))

    activatedAbility {
        val land = target(TargetFilter.Land.youControl())
        cost = AbilityCost.Tap
        effect = Effects.Pipeline {
            val landType = chooseOption(OptionType.BASIC_LAND_TYPE)
            run(Effects.SetLandType(target = land, fromChosen = landType, duration = Duration.EndOfTurn))
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "61"
        artist = "Anastasia Ovchinnikova"
        imageUri = "https://cards.scryfall.io/normal/front/1/7/171bfcb2-dd43-4d63-b2e2-6ca594c2ec40.jpg?1783921347"
        ruling("2022-09-09", "You choose a basic land type as the last ability resolves.")
        ruling("2022-09-09", "The affected land loses its existing land types and any abilities printed on it. It becomes the chosen basic land type and now has the ability to tap to add one mana of the appropriate color. Pixie Illusionist's ability doesn't change the affected land's name or whether it's legendary or basic.")
        ruling("2022-09-09", "Pixie Illusionist's third ability isn't a triggered ability. If it was kicked, there is no point where an opponent can respond while it is on the battlefield but doesn't yet have counters on it.")
    }
}
