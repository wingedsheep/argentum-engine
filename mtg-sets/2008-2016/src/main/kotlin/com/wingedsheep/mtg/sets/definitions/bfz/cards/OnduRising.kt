package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ondu Rising
 * {1}{W}
 * Sorcery
 * Whenever a creature attacks this turn, it gains lifelink until end of turn.
 * Awaken 4—{4}{W} (If you cast this spell for {4}{W}, also put four +1/+1 counters on target land
 * you control and it becomes a 0/0 Elemental creature with haste. It's still a land.)
 *
 * The spell creates a delayed trigger that lasts the turn and fires for every attacking creature,
 * whoever controls it — including the awakened land itself (ruling).
 */
val OnduRising = card("Ondu Rising") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Whenever a creature attacks this turn, it gains lifelink until end of turn.\n" +
        "Awaken 4—{4}{W} (If you cast this spell for {4}{W}, also put four +1/+1 counters on target " +
        "land you control and it becomes a 0/0 Elemental creature with haste. It's still a land.)"

    keywordAbility(KeywordAbility.awaken(4, "{4}{W}"))

    spell {
        effect = Effects.CreateDelayedTrigger(
            effect = Effects.GrantKeyword(Keyword.LIFELINK, EffectTarget.TriggeringEntity),
            trigger = Triggers.a(GameObjectFilter.Creature).attacks(),
            fireOnce = false,
            expiry = DelayedTriggerExpiry.EndOfTurn,
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "41"
        artist = "Igor Kieryluk"
        imageUri = "https://cards.scryfall.io/normal/front/0/7/07cb9f87-7ba4-408c-952d-fc9a7611df42.jpg?1783938217"
        ruling("2015-10-02", "Ondu Rising creates a delayed triggered ability. Any creature that attacks that turn will cause that ability to trigger, including creatures controlled by a teammate and creatures you didn't control or that didn't exist as Ondu Rising resolved. Notably, this includes the land creature created if Ondu Rising is cast for its awaken cost.")
    }
}
