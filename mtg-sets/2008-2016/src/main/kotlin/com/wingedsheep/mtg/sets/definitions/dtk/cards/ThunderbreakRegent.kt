package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Thunderbreak Regent
 * {2}{R}{R}
 * Creature — Dragon
 * 4/4
 *
 * Flying
 * Whenever a Dragon you control becomes the target of a spell or ability an opponent controls,
 * this creature deals 3 damage to that player.
 *
 * An observer becomes-target trigger over Dragons you control (Thunderbreak included — `Triggers.a`
 * watches the source too), narrowed to opponents' spells and abilities. "That player" is the
 * targeting spell or ability's controller, [Player.ControllerOfCause] — the becomes-target trigger
 * binds the targeted Dragon as its triggering entity, so this is the only handle on the other end
 * of the targeting (Fractured Loyalty's reference). The trigger resolves before the spell or
 * ability that caused it (ruling).
 */
val ThunderbreakRegent = card("Thunderbreak Regent") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Dragon"
    power = 4
    toughness = 4
    oracleText = "Flying\n" +
        "Whenever a Dragon you control becomes the target of a spell or ability an opponent controls, " +
        "this creature deals 3 damage to that player."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Permanent.withSubtype(Subtype.DRAGON).youControl())
            .becomesTarget(byOpponent = true)
        effect = Effects.DealDamage(3, EffectTarget.PlayerRef(Player.ControllerOfCause))
        description = "Whenever a Dragon you control becomes the target of a spell or ability an " +
            "opponent controls, this creature deals 3 damage to that player."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "162"
        artist = "Ryan Pancoast"
        flavorText = "Attracting a dragon's attention may be the last mistake you make."
        imageUri = "https://cards.scryfall.io/normal/front/a/2/a2b9cf2b-efe3-4815-be18-c3b69ee762f2.jpg?1783938585"
        ruling("2015-02-25", "Thunderbreak Regent's ability will resolve before the spell or ability that caused it to trigger.")
    }
}
