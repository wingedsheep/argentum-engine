package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Turret Ogre — War of the Spark #148 (canonical printing)
 * {3}{R} · Creature — Ogre Warrior · 4/3
 *
 * Reach
 * When this creature enters, if you control another creature with power 4 or greater, this
 * creature deals 2 damage to each opponent.
 *
 * The intervening-if excludes the Ogre itself (`excludeSelf = true`) — it is power 4 on its own,
 * so without the "another" exclusion it would always satisfy its own condition. Checked as the
 * trigger would fire and again on resolution; the creature need not be the same one (ruling).
 */
val TurretOgre = card("Turret Ogre") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Ogre Warrior"
    oracleText = "Reach (This creature can block creatures with flying.)\n" +
        "When this creature enters, if you control another creature with power 4 or greater, this creature deals 2 damage to each opponent."
    power = 4
    toughness = 3

    keywords(Keyword.REACH)

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.YouControl(GameObjectFilter.Creature.powerAtLeast(4), excludeSelf = true)
        effect = Effects.DealDamage(2, EffectTarget.PlayerRef(Player.EachOpponent))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "148"
        artist = "Johann Bodin"
        flavorText = "Common Gruul career aspirations: berserker, shaman, catapult."
        imageUri = "https://cards.scryfall.io/normal/front/8/b/8b5f624f-c5eb-4150-b36e-6147b440ff56.jpg?1783933418"
        ruling("2020-06-23", "If you don't control another creature with power 4 or greater immediately after Turret Ogre enters the battlefield, its ability doesn't trigger, even if you can raise a creature's power right away. If you control no other creature with power 4 or greater as the ability resolves, nothing happens. These don't have to be the same creature, though.")
        ruling("2020-06-23", "Turret Ogre's ability doesn't deal more than 2 damage to each opponent if you control more than one other creature with power 4 or greater.")
    }
}
