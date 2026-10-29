package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Devoted Druid — Shadowmoor #110
 * {1}{G} · Creature — Elf Druid · 0/2
 *
 * {T}: Add {G}.
 * Put a -1/-1 counter on this creature: Untap this creature.
 *
 * The -1/-1 counter is an activation cost ([Costs.PutCounterOnSelf]), so it lands before the untap
 * resolves: the second activation leaves a 0/0 that dies to state-based actions before it can untap.
 * Under Solemnity the counter can't be put on it, so the untap can't be activated at all (CR
 * 614.17b); under Vizier of Remedies the counter is replaced to nothing and the untap is free.
 */
val DevotedDruid = card("Devoted Druid") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Druid"
    power = 0
    toughness = 2
    oracleText = "{T}: Add {G}.\nPut a -1/-1 counter on this creature: Untap this creature."

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.GREEN)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.PutCounterOnSelf(CounterType.MINUS_ONE_MINUS_ONE)
        effect = Effects.Untap(EffectTarget.Self)
        description = "Put a -1/-1 counter on this creature: Untap this creature."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "110"
        artist = "Darrell Riche"
        flavorText = "\"Mana lurks in all things, even in the depths of one's own heart.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/2/820e2f07-f637-4144-b45a-0e1430dcf55e.jpg?1783942744"
        ruling(
            "2018-12-07",
            "If you can't put -1/-1 counters on Devoted Druid (due to an effect such as that of " +
                "Solemnity), you can't activate its second ability. If you can put counters on it, " +
                "but that is modified by an effect (such as that of Vizier of Remedies), you can " +
                "activate the ability even if paying the cost causes no counters to be put on " +
                "Devoted Druid."
        )
        ruling(
            "2018-12-07",
            "You put the -1/-1 counter on Devoted Druid as a cost to activate its ability, not when " +
                "it resolves. If paying the cost causes Devoted Druid to have 0 toughness, it's put " +
                "into your graveyard before you can untap it and before you can even pay the cost again."
        )
    }
}
