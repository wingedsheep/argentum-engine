package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Hostile Hostel // Creeping Inn (Innistrad: Midnight Hunt)
 * Land // Artifact Creature — Horror Construct 3/7 (transform)
 *
 * Front — Hostile Hostel
 *   {T}: Add {C}.
 *   {1}, {T}, Sacrifice a creature: Put a soul counter on this land. Then if there are three or
 *   more soul counters on it, remove those counters, transform it, then untap it. Activate only
 *   as a sorcery.
 *
 * Back — Creeping Inn
 *   Whenever this creature attacks, you may exile a creature card from your graveyard. If you do,
 *   each opponent loses X life and you gain X life, where X is the number of creature cards
 *   exiled with this creature.
 *   {4}: This creature phases out.
 *
 * The front ability is Treasure Map's "add a counter, then if three or more" shape; "remove those
 * counters" removes every soul counter ([Effects.RemoveAllCountersOfType]), not just three.
 *
 * The attack trigger exiles the chosen card linked to the permanent (`linkToSource`, CR 607), so
 * X counts every creature card still in exile that this permanent exiled — the just-exiled one
 * included. The link lives on the permanent, so it survives phasing (per the ruling). The loss
 * and the gain are separate events: you gain X once, however many opponents lose X.
 */

private val creatureCardsExiledWithIt = GameObjectFilter.Creature.exiledWithSource()

private val HostileHostelFront = card("Hostile Hostel") {
    manaCost = ""
    colorIdentity = "B"
    typeLine = "Land"
    oracleText = "{T}: Add {C}.\n" +
        "{1}, {T}, Sacrifice a creature: Put a soul counter on this land. Then if there are three or " +
        "more soul counters on it, remove those counters, transform it, then untap it. Activate only " +
        "as a sorcery."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}"),
            Costs.Tap,
            Costs.Sacrifice(GameObjectFilter.Creature),
        )
        effect = Effects.AddCounters(CounterType.SOUL, 1, EffectTarget.Self) then
            Effects.If(
                condition = Conditions.SourceCounterCountAtLeast(CounterType.SOUL, 3),
                then = Effects.RemoveAllCountersOfType(CounterType.SOUL, EffectTarget.Self) then
                    Effects.Transform(EffectTarget.Self) then
                    Effects.Untap(EffectTarget.Self),
            )
        timing = TimingRule.SorcerySpeed
        description = "{1}, {T}, Sacrifice a creature: Put a soul counter on this land. Then if there " +
            "are three or more soul counters on it, remove those counters, transform it, then untap it."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "264"
        artist = "Daniel Ljunggren"
        imageUri = "https://cards.scryfall.io/normal/front/a/c/ac83c27f-55d6-4e5a-93a4-febb0c183289.jpg?1783925549"
        ruling("2021-09-24", "While Creeping Inn is phased out, it's treated as though it doesn't exist. It can't be the target of spells or abilities, its static abilities have no effect on the game, its triggered abilities can't trigger, it can't attack or block, and so on.")
        ruling("2021-09-24", "If Creeping Inn is attacking or blocking when it phases out, it is removed from combat.")
        ruling("2021-09-24", "Creeping Inn will phase in during its controller's untap step. It will be able to attack the same turn.")
        ruling("2021-09-24", "When Creeping Inn phases in, it still remembers what cards were previously exiled with it.")
        ruling("2021-09-24", "Phasing out or phasing in doesn't affect which face of a double-faced card is face up.")
    }
}

private const val ATTACK_TEXT = "Whenever this creature attacks, you may exile a creature card from your " +
    "graveyard. If you do, each opponent loses X life and you gain X life, where X is the number of " +
    "creature cards exiled with this creature."

private val CreepingInn = card("Creeping Inn") {
    manaCost = ""
    colorIdentity = "B"
    colorIndicator = "B" // Transformed back face, no mana cost (CR 204).
    typeLine = "Artifact Creature — Horror Construct"
    power = 3
    toughness = 7
    oracleText = "$ATTACK_TEXT\n{4}: This creature phases out."

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.Pipeline {
            val creatureCards = gather(
                CardSource.FromZone(
                    zone = Zone.GRAVEYARD,
                    player = Player.You,
                    filter = GameObjectFilter.Creature,
                )
            )
            val exiled = chooseUpTo(
                1,
                from = creatureCards,
                chooser = Chooser.Controller,
                prompt = "You may exile a creature card from your graveyard",
                selectedLabel = "Exile",
                alwaysPrompt = true,
            )
            exile(exiled, linkToSource = true)
            ifNotEmpty(exiled) {
                val x = DynamicAmounts.count(Player.Each, Zone.EXILE, creatureCardsExiledWithIt)
                run(Effects.LoseLife(x, EffectTarget.PlayerRef(Player.EachOpponent)))
                run(Effects.GainLife(x))
            }
        }
        description = ATTACK_TEXT
    }

    activatedAbility {
        cost = Costs.Mana("{4}")
        effect = Effects.PhaseOut(EffectTarget.Self)
        description = "{4}: This creature phases out."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "264"
        artist = "Daniel Ljunggren"
        imageUri = "https://cards.scryfall.io/normal/back/a/c/ac83c27f-55d6-4e5a-93a4-febb0c183289.jpg?1783925549"
    }
}

val HostileHostel: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = HostileHostelFront,
    backFace = CreepingInn,
)
