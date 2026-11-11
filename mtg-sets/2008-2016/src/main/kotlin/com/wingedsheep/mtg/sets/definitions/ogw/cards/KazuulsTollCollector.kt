package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Kazuul's Toll Collector — Oath of the Gatewatch #112
 * {2}{R} · Creature — Ogre Warrior · 3/2 · Uncommon
 *
 * {0}: Attach target Equipment you control to this creature. Activate only as a sorcery.
 *
 * A free, sorcery-speed equip for any Equipment you control, onto this creature only. The
 * Equipment is the ability's one target; the host is the source ([EffectTarget.Self]), so
 * [Effects.AttachTargetEquipmentToCreature] does the move — detaching it from any other creature
 * first. An Equipment that can't legally be attached to this creature doesn't move, and one already
 * attached to it stays put (CR 701.3b). The Equipment's own equip cost is never paid.
 */
val KazuulsTollCollector = card("Kazuul's Toll Collector") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Ogre Warrior"
    power = 3
    toughness = 2
    oracleText = "{0}: Attach target Equipment you control to this creature. Activate only as a sorcery."

    activatedAbility {
        cost = Costs.Free
        val equipment = target(
            TargetFilter(baseFilter = GameObjectFilter.Artifact.withSubtype(Subtype.EQUIPMENT).youControl()),
        )
        effect = Effects.AttachTargetEquipmentToCreature(
            equipmentTarget = equipment,
            creatureTarget = EffectTarget.Self,
        )
        timing = TimingRule.SorcerySpeed
        description = "Attach target Equipment you control to this creature"
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "112"
        artist = "Greg Staples"
        flavorText = "Travelers are advised to bring extra swords, preferably shiny ones."
        imageUri = "https://cards.scryfall.io/normal/front/6/8/6824f48d-6808-4458-a2fd-1ffd0a10d72a.jpg?1783937905"
    }
}
