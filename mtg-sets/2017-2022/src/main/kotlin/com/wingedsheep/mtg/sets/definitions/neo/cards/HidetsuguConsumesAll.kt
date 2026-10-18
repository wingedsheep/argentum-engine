package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Hidetsugu Consumes All // Vessel of the All-Consuming
 * {1}{B}{R} — Enchantment — Saga // Enchantment Creature — Ogre Shaman 3/3
 *
 * Front — Hidetsugu Consumes All:
 *   I — Destroy each nonland permanent with mana value 1 or less.
 *   II — Exile all graveyards.
 *   III — Exile this Saga, then return it to the battlefield transformed under your control.
 *
 * Back — Vessel of the All-Consuming:
 *   Trample
 *   Whenever this creature deals damage, put a +1/+1 counter on it.
 *   Whenever this creature deals damage to a player, if it has dealt 10 or more damage to that
 *   player this turn, they lose the game.
 *
 * The first trigger has no recipient restriction, so trampling over a blocker (or hitting several
 * blockers) triggers it once, as the ruling requires. The second reads the Vessel's own per-player
 * damage tally for the turn (`DynamicAmounts.damageDealtBySourceThisTurn`), combat and noncombat
 * alike, as an intervening "if" — checked when it triggers and again on resolution.
 */
private val VesselOfTheAllConsuming = card("Vessel of the All-Consuming") {
    manaCost = ""
    colorIndicator = "BR"
    colorIdentity = "BR"
    typeLine = "Enchantment Creature — Ogre Shaman"
    oracleText = "Trample\n" +
        "Whenever this creature deals damage, put a +1/+1 counter on it.\n" +
        "Whenever this creature deals damage to a player, if it has dealt 10 or more damage to that " +
        "player this turn, they lose the game."
    power = 3
    toughness = 3

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.self.dealsDamage()
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    triggeredAbility {
        trigger = Triggers.self.dealsDamage(Recipient.AnyPlayer)
        interveningIf = Conditions.CompareAmounts(
            DynamicAmounts.damageDealtBySourceThisTurn(Player.TriggeringPlayer),
            ComparisonOperator.GTE,
            10
        )
        effect = Effects.LoseGame(EffectTarget.PlayerRef(Player.TriggeringPlayer))
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "221"
        artist = "Chris Cold"
        imageUri = "https://cards.scryfall.io/normal/back/8/e/8e910464-329a-4de1-930a-be85b1956676.jpg?1783923841"
    }
}

private val HidetsuguConsumesAllFront = card("Hidetsugu Consumes All") {
    manaCost = "{1}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter.)\n" +
        "I — Destroy each nonland permanent with mana value 1 or less.\n" +
        "II — Exile all graveyards.\n" +
        "III — Exile this Saga, then return it to the battlefield transformed under your control."

    sagaChapter(1) {
        effect = Effects.DestroyAll(GameObjectFilter.NonlandPermanent.manaValueAtMost(1))
    }

    sagaChapter(2) {
        effect = Effects.Pipeline {
            exile(gather(CardSource.FromZone(Zone.GRAVEYARD, Player.Each)))
        }
    }

    sagaChapter(3) {
        effect = Effects.ExileAndReturnTransformed()
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "221"
        artist = "Chris Cold"
        imageUri = "https://cards.scryfall.io/normal/front/8/e/8e910464-329a-4de1-930a-be85b1956676.jpg?1783923841"
        ruling(
            "2022-02-18",
            "Vessel of the All-Consuming's first triggered ability will trigger whenever it deals any damage, " +
                "not just combat damage. If it deals damage to multiple players and/or permanents at the same " +
                "time (perhaps because of trample or because it was blocked by multiple creatures), that " +
                "ability triggers only once."
        )
        ruling(
            "2022-02-18",
            "If Vessel of the All-Consuming is dealt lethal damage at the same time it deals damage, it will " +
                "be destroyed before the first triggered ability resolves, so the +1/+1 counter won't be put " +
                "on it in time to potentially save it."
        )
    }
}

val HidetsuguConsumesAll: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = HidetsuguConsumesAllFront,
    backFace = VesselOfTheAllConsuming,
)
