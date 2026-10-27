package com.wingedsheep.mtg.sets.definitions.c17.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Patron of the Vein
 * {4}{B}{B}
 * Creature — Vampire Shaman
 * 4/4
 * Flying
 * When this creature enters, destroy target creature an opponent controls.
 * Whenever a creature an opponent controls dies, exile it and put a +1/+1 counter on each Vampire
 * you control.
 *
 * The death trigger exiles the dead card from its graveyard ([EffectTarget.TriggeringEntity]) and
 * then counts every Vampire you control at resolution — the counters land even when the card can't
 * be exiled (a token, or a card that already left the graveyard), per the ruling.
 */
val PatronOfTheVein = card("Patron of the Vein") {
    manaCost = "{4}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire Shaman"
    power = 4
    toughness = 4
    oracleText = "Flying\n" +
        "When this creature enters, destroy target creature an opponent controls.\n" +
        "Whenever a creature an opponent controls dies, exile it and put a +1/+1 counter on each Vampire you control."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val victim = target(TargetFilter.CreatureOpponentControls)
        effect = Effects.Destroy(victim)
        description = "When this creature enters, destroy target creature an opponent controls."
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.opponentControls()).dies()
        effect = Effects.Exile(EffectTarget.TriggeringEntity, fromZone = Zone.GRAVEYARD) then
            Effects.ForEachInGroup(
                filter = GroupFilter(GameObjectFilter.Permanent.withSubtype(Subtype.VAMPIRE).youControl()),
                effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.IterationEntity),
            )
        description = "Whenever a creature an opponent controls dies, exile it and put a +1/+1 counter on each Vampire you control."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "20"
        artist = "Tommy Arnold"
        imageUri = "https://cards.scryfall.io/normal/front/e/2/e27db39c-c4b8-46b9-afe6-1737841bdad6.jpg?1783935944"
        ruling("2017-08-25", "Destroying a creature with Patron of the Vein's first ability causes its last ability to trigger if Patron of the Vein is still on the battlefield.")
        ruling("2017-08-25", "Patron of the Vein's last ability puts a +1/+1 counter on each Vampire you control, including itself, even if the creature that died can't be exiled (most likely because it was a token).")
        ruling("2017-08-25", "If Patron of the Vein and a creature an opponent controls die simultaneously (perhaps because they fought or were in combat together), Patron of the Vein's last ability triggers, but Patron of the Vein won't be on the battlefield as that ability resolves. It can't be saved by the +1/+1 counter that would have been put on it. The same is true of any other Vampires you control that die at the same time. Your remaining Vampires will each get a +1/+1 counter.")
    }
}
