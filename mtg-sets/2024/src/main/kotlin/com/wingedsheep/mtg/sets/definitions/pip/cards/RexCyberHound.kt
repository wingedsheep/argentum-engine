package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DonorCards
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.HasAllActivatedAbilitiesOfCards
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Rex, Cyber-Hound
 * {1}{W}{U}
 * Legendary Artifact Creature — Robot Dog
 * 2/2
 *
 * Whenever Rex deals combat damage to a player, they mill two cards and you get {E}{E}.
 * Pay {E}{E}: Choose target creature card in a graveyard. Exile it with a brain counter on it.
 * Activate only as a sorcery.
 * Rex has all activated abilities of all cards in exile with brain counters on them.
 *
 * The last line is the `ALL_EXILE` pool of [HasAllActivatedAbilitiesOfCards], narrowed to
 * brain-countered cards — any card in exile with one counts, not only the ones Rex exiled.
 */
val RexCyberHound = card("Rex, Cyber-Hound") {
    manaCost = "{1}{W}{U}"
    colorIdentity = "WU"
    typeLine = "Legendary Artifact Creature — Robot Dog"
    power = 2
    toughness = 2
    oracleText = "Whenever Rex deals combat damage to a player, they mill two cards and you get {E}{E} (two energy counters).\n" +
        "Pay {E}{E}: Choose target creature card in a graveyard. Exile it with a brain counter on it. Activate only as a sorcery.\n" +
        "Rex has all activated abilities of all cards in exile with brain counters on them."

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Patterns.Library.mill(2, EffectTarget.PlayerRef(Player.TriggeringPlayer)) then
            Effects.GetEnergy(2)
    }

    activatedAbility {
        cost = Costs.PayPlayerCounters(CounterType.ENERGY, 2)
        timing = TimingRule.SorcerySpeed
        val creatureCard = target(TargetFilter.CreatureInGraveyard)
        effect = Effects.Exile(creatureCard, fromZone = Zone.GRAVEYARD, addCounterType = CounterType.BRAIN)
        description = "Pay {E}{E}: Exile target creature card in a graveyard with a brain counter on it. " +
            "Activate only as a sorcery."
    }

    staticAbility {
        ability = HasAllActivatedAbilitiesOfCards(
            donors = DonorCards.ALL_EXILE,
            cardFilter = GameObjectFilter.Any.withCounter(CounterType.BRAIN)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "117"
        artist = "Jeremy Paillotin"
        imageUri = "https://cards.scryfall.io/normal/front/d/a/dae95822-d4a9-4698-bd11-c504414fc160.jpg?1783912385"
        ruling("2024-03-08", "Rex gains only activated abilities. It doesn't gain triggered abilities or static abilities. Activated abilities contain a colon. They're generally written \"[Cost]: [Effect].\" Some keywords are activated abilities and may have colons in their reminder text.")
        ruling("2024-03-08", "If an activated ability of a card in exile with a brain counter on it references the card it's printed on by name, treat Rex's version of that ability as though it referenced Rex, Cyber-Hound instead. For example, say Arcade Gannon (which has the ability \"{T}: Draw a card, then discard a card. Put a quest counter on Arcade Gannon.\") is in exile with a brain counter on it. You would treat Rex's version of the ability as though it said \"{T}: Draw a card, then discard a card. Put a quest counter on Rex, Cyber-Hound.\"")
    }
}
