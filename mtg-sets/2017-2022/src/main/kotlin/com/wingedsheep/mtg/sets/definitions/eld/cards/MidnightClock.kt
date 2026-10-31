package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Midnight Clock — Throne of Eldraine #54
 * {2}{U} · Artifact · Rare
 *
 * {T}: Add {U}.
 * {2}{U}: Put an hour counter on this artifact.
 * At the beginning of each upkeep, put an hour counter on this artifact.
 * When the twelfth hour counter is put on this artifact, shuffle your hand and graveyard into your
 * library, then draw seven cards. Exile this artifact.
 *
 * The last ability is the ordinal counters-placed template (`getsCounters(HOUR, nth = 12)`): it
 * fires for the placement that takes the Clock from fewer than twelve hour counters to twelve or
 * more (ruling 2019-10-04), not for later ones. If the Clock has left the battlefield by
 * resolution, the exile finds a new object and does nothing (ruling 2019-10-04).
 */
val MidnightClock = card("Midnight Clock") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Artifact"
    oracleText = "{T}: Add {U}.\n" +
        "{2}{U}: Put an hour counter on this artifact.\n" +
        "At the beginning of each upkeep, put an hour counter on this artifact.\n" +
        "When the twelfth hour counter is put on this artifact, shuffle your hand and graveyard into your library, " +
        "then draw seven cards. Exile this artifact."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLUE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Mana("{2}{U}")
        effect = Effects.AddCounters(CounterType.HOUR, 1, EffectTarget.Self)
    }

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.UPKEEP)
        effect = Effects.AddCounters(CounterType.HOUR, 1, EffectTarget.Self)
    }

    triggeredAbility {
        trigger = Triggers.self.getsCounters(CounterType.HOUR, nth = 12)
        effect = Effects.Pipeline {
            val midnightClockReset = gather(
                CardSource.FromMultipleZones(zones = listOf(Zone.HAND, Zone.GRAVEYARD), player = Player.You)
            )
            move(midnightClockReset, CardDestination.ToZone(Zone.LIBRARY, Player.You, ZonePlacement.Shuffled))
            run(Effects.DrawCards(7))
            run(Effects.Exile(EffectTarget.Self))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "54"
        artist = "Alexander Forssberg"
        imageUri = "https://cards.scryfall.io/normal/front/0/f/0f7f1148-7b1b-4969-a2f8-428de1e2e8ff.jpg?1783932655"
        ruling("2019-10-04", "If Midnight Clock leaves the battlefield while its last ability is on the stack, it won't be exiled.")
        ruling("2019-10-04", "You can activate Midnight Clock's mana ability to pay the cost of its second ability.")
        ruling("2019-10-04", "Midnight Clock's last ability triggers after one or more counters are put onto it if it had fewer than twelve counters on it before those counters were put on it and it has twelve or more counters on it after.")
        ruling("2019-10-04", "Midnight Clock's first triggered ability triggers at the beginning of each upkeep, not just your upkeep. In a Two-Headed Giant game, an ability that triggers at the beginning of each upkeep rather than each player's upkeep triggers only once during each team's upkeep.")
    }
}
