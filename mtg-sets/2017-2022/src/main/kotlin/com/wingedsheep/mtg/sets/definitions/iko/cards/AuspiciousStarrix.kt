package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty

/**
 * Auspicious Starrix
 * {4}{G}
 * Creature — Elk Beast
 * 6/6
 * Mutate {5}{G}
 * Whenever this creature mutates, exile cards from the top of your library until you exile X
 * permanent cards, where X is the number of times this creature has mutated. Put those permanent
 * cards onto the battlefield.
 *
 * "Permanent cards" includes lands. Every card walked past is exiled; the X permanent cards then
 * move from exile to the battlefield, and anything that can't (an Aura with nothing to enchant)
 * stays in exile alongside the instants and sorceries. A short library exiles all of it and puts
 * whatever permanent cards it held onto the battlefield.
 */
val AuspiciousStarrix = card("Auspicious Starrix") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elk Beast"
    power = 6
    toughness = 6
    oracleText = "Mutate {5}{G} (If you cast this spell for its mutate cost, put it over or under target non-Human creature you own. They mutate into the creature on top plus all abilities from under it.)\n" +
        "Whenever this creature mutates, exile cards from the top of your library until you exile X permanent cards, where X is the number of times this creature has mutated. Put those permanent cards onto the battlefield."

    keywordAbility(KeywordAbility.mutate("{5}{G}"))

    triggeredAbility {
        trigger = Triggers.self.mutates()
        val timesMutated = DynamicAmounts.propertyOf(EffectTarget.Self, EntityNumericProperty.TimesMutated)
        effect = Effects.Pipeline {
            val (found, exiled) = gatherUntilMatch(GameObjectFilter.Permanent, count = timesMutated)
            exile(exiled)
            move(found, CardDestination.ToZone(Zone.BATTLEFIELD, Player.You))
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "144"
        artist = "Lucas Graciano"
        imageUri = "https://cards.scryfall.io/normal/front/a/3/a39ae1e4-d4dd-4691-af5a-5fa25ace4ebe.jpg?1783931040"
        ruling("2020-04-17", "Auspicious Starrix's effect doesn't count as playing lands. It can put land cards onto the battlefield even if it's not your turn or if you've already played your land for the turn.")
        ruling("2020-04-17", "If there are fewer than X permanent cards in your library, you exile your entire library and put the permanent cards in it onto the battlefield.")
        ruling("2020-04-17", "Any cards that you exile but don't put on the battlefield for any reason (for example, because the card is an instant card or an Aura that can't enchant anything) remain in exile.")
    }
}
