package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Sensei Golden-Tail
 * {1}{W}
 * Legendary Creature — Fox Samurai
 * 2/1
 * Bushido 1 (Whenever this creature blocks or becomes blocked, it gets +1/+1 until end of turn.)
 * {1}{W}, {T}: Put a training counter on target creature. That creature gains bushido 1 and
 * becomes a Samurai in addition to its other creature types. Activate only as a sorcery.
 *
 * The grant is permanent and independent of the counter — the ruling: "The training counter just
 * marks which creatures have been changed; an effect that removes the counter doesn't change the
 * creature's types or abilities." [Effects.GrantBushido] floats `BUSHIDO_1`, from which the engine
 * derives the bushido trigger; activating twice on one creature gives it bushido 2 in total.
 */
val SenseiGoldenTail = card("Sensei Golden-Tail") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Fox Samurai"
    power = 2
    toughness = 1
    oracleText = "Bushido 1 (Whenever this creature blocks or becomes blocked, it gets +1/+1 until end of turn.)\n" +
        "{1}{W}, {T}: Put a training counter on target creature. That creature gains bushido 1 and " +
        "becomes a Samurai in addition to its other creature types. Activate only as a sorcery."

    keywordAbility(KeywordAbility.bushido(1))

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{W}"), Costs.Tap)
        val t = target(TargetFilter.Creature)
        effect = Effects.AddCounters(CounterType.TRAINING, 1, t) then
            Effects.GrantBushido(1, t, Duration.Permanent) then
            Effects.AddCreatureType(Subtype.SAMURAI.value, t, Duration.Permanent)
        timing = TimingRule.SorcerySpeed
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "44"
        artist = "Stephen Tappin"
        imageUri = "https://cards.scryfall.io/normal/front/b/9/b93dd897-3fb5-48b1-bdff-9383ce1feb21.jpg?1783944332"
    }
}
