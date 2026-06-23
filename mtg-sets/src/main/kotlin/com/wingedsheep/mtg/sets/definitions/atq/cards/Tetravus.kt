package com.wingedsheep.mtg.sets.definitions.atq.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ConvertCountersToTokensEffect
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Tetravus
 * {6}
 * Artifact Creature — Construct
 * 1/1
 *
 * Flying
 * This creature enters with three +1/+1 counters on it.
 * At the beginning of your upkeep, you may remove any number of +1/+1 counters from this creature.
 * If you do, create that many 1/1 colorless Tetravite artifact creature tokens. They each have
 * flying and "This token can't be enchanted."
 * At the beginning of your upkeep, you may exile any number of tokens created with this creature.
 * If you do, put that many +1/+1 counters on this creature.
 *
 * Implementation: counter↔token conversion in both directions.
 *  - Counters → tokens: [ConvertCountersToTokensEffect] prompts for the number to remove, removes
 *    them, and mints that many Tetravite tokens (flying + can't-be-enchanted), each `stampCreator`-
 *    stamped so they're recognizable as "created with this creature".
 *  - Tokens → counters: composed from atoms — gather the Tetravite tokens this creature created,
 *    on any player's battlefield ([CardSource.BattlefieldMatching] with `.createdBySource()` as the
 *    sole filter — the oracle has no "you control" clause), let the player choose any number, exile
 *    them, and put that many +1/+1 counters back on Tetravus.
 * Both upkeep abilities are "you may" (optional triggers).
 */
val Tetravus = card("Tetravus") {
    manaCost = "{6}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Construct"
    power = 1
    toughness = 1
    keywords(Keyword.FLYING)
    oracleText = "Flying\n" +
        "This creature enters with three +1/+1 counters on it.\n" +
        "At the beginning of your upkeep, you may remove any number of +1/+1 counters from this " +
        "creature. If you do, create that many 1/1 colorless Tetravite artifact creature tokens. " +
        "They each have flying and \"This token can't be enchanted.\"\n" +
        "At the beginning of your upkeep, you may exile any number of tokens created with this " +
        "creature. If you do, put that many +1/+1 counters on this creature."

    replacementEffect(
        EntersWithCounters(
            counterType = CounterTypeFilter.PlusOnePlusOne,
            count = 3,
            selfOnly = true
        )
    )

    // Upkeep: remove any number of +1/+1 counters; create that many Tetravite tokens.
    triggeredAbility {
        trigger = Triggers.YourUpkeep
        optional = true
        effect = ConvertCountersToTokensEffect(
            counterType = CounterTypeFilter.PlusOnePlusOne,
            tokenFactory = CreateTokenEffect(
                count = DynamicAmount.Fixed(1),
                power = 1,
                toughness = 1,
                colors = emptySet(),
                creatureTypes = setOf("Tetravite"),
                keywords = setOf(Keyword.FLYING),
                name = "Tetravite",
                artifactToken = true,
                staticAbilities = listOf(
                    GrantKeyword(AbilityFlag.CANT_BE_ENCHANTED.name, GroupFilter.source())
                ),
                stampCreator = true
            )
        )
    }

    // Upkeep: exile any number of tokens created with this creature; add that many +1/+1 counters.
    triggeredAbility {
        trigger = Triggers.YourUpkeep
        optional = true
        effect = Effects.Pipeline {
            val mine = gather(
                CardSource.BattlefieldMatching(
                    filter = GameObjectFilter.Any.createdBySource(),
                    player = Player.Each
                ),
                name = "tetraviteTokens"
            )
            val chosen = chooseAnyNumber(
                from = mine,
                name = "exiledTokens",
                prompt = "Exile any number of Tetravite tokens created with this creature"
            )
            exile(chosen)
            run(
                Effects.AddDynamicCounters(
                    counterType = "+1/+1",
                    amount = DynamicAmount.VariableReference("${chosen.key}_count"),
                    target = EffectTarget.Self
                )
            )
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "71"
        artist = "Mark Tedin"
        imageUri = "https://cards.scryfall.io/normal/front/2/3/23eb19f9-2e8f-4bf0-9bf8-868e6da70e2d.jpg?1562902619"
    }
}
