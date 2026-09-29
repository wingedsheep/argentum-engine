package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetOther

/**
 * Invasion of Ulgrotha // Grandmother Ravi Sengir — March of the Machine #116.
 * {4}{B} · Battle — Siege · defense 5 // Legendary Creature — Human Wizard 3/3
 *
 * The front's "any other target" is [Targets.Any] wrapped in [TargetOther] (the Siege can't hit
 * itself). The back's opponent-creature dies trigger adds a +1/+1 counter and gains 1 life.
 */
private val InvasionOfUlgrothaFront = card("Invasion of Ulgrotha") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Battle — Siege"
    startingDefense = 5
    oracleText = "(As a Siege enters, choose an opponent to protect it. You and others can attack " +
        "it. When it's defeated, exile it, then cast it transformed.)\n" +
        "When this Siege enters, it deals 3 damage to any other target and you gain 3 life."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val victim = target(TargetOther(Targets.Any))
        effect = Effects.DealDamage(3, victim) then Effects.GainLife(3)
        description = "When this Siege enters, it deals 3 damage to any other target and you gain 3 life."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "116"
        artist = "Viko Menezes"
        imageUri = "https://cards.scryfall.io/normal/front/4/9/49ec5e00-22f4-486e-8e99-e950725f6fbc.jpg?1783917012"
    }
}

private val GrandmotherRaviSengir = card("Grandmother Ravi Sengir") {
    manaCost = ""
    colorIdentity = "B"
    colorIndicator = "B"
    typeLine = "Legendary Creature — Human Wizard"
    power = 3
    toughness = 3
    oracleText = "Flying\nWhenever a creature an opponent controls dies, put a +1/+1 counter on " +
        "Grandmother Ravi Sengir and you gain 1 life."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.opponentControls()).dies()
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self) then
            Effects.GainLife(1)
        description = "Whenever a creature an opponent controls dies, put a +1/+1 counter on " +
            "Grandmother Ravi Sengir and you gain 1 life."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "116"
        artist = "Viko Menezes"
        flavorText = "\"I do enjoy a good apocalypse.\""
        imageUri = "https://cards.scryfall.io/normal/back/4/9/49ec5e00-22f4-486e-8e99-e950725f6fbc.jpg?1783917012"
        ruling("2023-04-14", "If creatures controlled by opponents die at the same time as Grandmother Ravi Sengir, the last ability of Grandmother Ravi Sengir will trigger once for each of those creatures. You won't put +1/+1 counters on it, but you will gain some life.")
    }
}

val InvasionOfUlgrotha: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = InvasionOfUlgrothaFront,
    backFace = GrandmotherRaviSengir,
)
