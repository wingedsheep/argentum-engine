package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttackUnless
import com.wingedsheep.sdk.scripting.CantBlockUnless
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Rhonas the Indomitable
 * {2}{G}
 * Legendary Creature — God
 * 5/5
 * Deathtouch, indestructible
 * Rhonas can't attack or block unless you control another creature with power 4 or greater.
 * {2}{G}: Another target creature gets +2/+0 and gains trample until end of turn.
 *
 * "Can't attack or block" is two restrictions over one condition; `excludeSelf` makes it
 * "*another* creature" — Rhonas's own power 5 must never satisfy it.
 */
val RhonasTheIndomitable = card("Rhonas the Indomitable") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — God"
    power = 5
    toughness = 5
    oracleText = "Deathtouch, indestructible\nRhonas can't attack or block unless you control another creature with power 4 or greater.\n{2}{G}: Another target creature gets +2/+0 and gains trample until end of turn."

    keywords(Keyword.DEATHTOUCH, Keyword.INDESTRUCTIBLE)

    val anotherBigCreature = Conditions.YouControl(
        GameObjectFilter.Creature.powerAtLeast(4),
        excludeSelf = true
    )

    staticAbility {
        ability = CantAttackUnless(anotherBigCreature)
    }
    staticAbility {
        ability = CantBlockUnless(anotherBigCreature)
    }

    activatedAbility {
        cost = Costs.Mana("{2}{G}")
        val other = target(TargetFilter.OtherCreature)
        effect = Effects.ModifyStats(2, 0, other) then Effects.GrantKeyword(Keyword.TRAMPLE, other)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "182"
        artist = "Chase Stone"
        imageUri = "https://cards.scryfall.io/normal/front/5/0/50f23c47-278c-4188-8fb4-ae20a0c423c5.jpg?1783936470"
        ruling("2017-04-18", "Once Rhonas has attacked or blocked, it will remain in combat even if you no longer control another creature with power 4 or greater.")
        ruling("2017-04-18", "You don't have to attack with another creature with power 4 or greater for Rhonas to be able to attack. The same is true of blocking.")
    }
}
