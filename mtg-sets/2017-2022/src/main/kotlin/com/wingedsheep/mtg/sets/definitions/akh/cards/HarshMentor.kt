package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Harsh Mentor
 * {1}{R}
 * Creature — Human Cleric
 * 2/2
 * Whenever an opponent activates an ability of an artifact, creature, or land on the battlefield,
 * if it isn't a mana ability, this creature deals 2 damage to that player.
 *
 * `.onBattlefield()` keeps cycling and graveyard abilities (embalm) out, per the ruling. A source
 * its own cost sacrifices — a fetch land, a Clue — is matched as it last existed on the
 * battlefield (CR 113.7a). The default activation gate already excludes mana abilities.
 */
val HarshMentor = card("Harsh Mentor") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Cleric"
    power = 2
    toughness = 2
    oracleText = "Whenever an opponent activates an ability of an artifact, creature, or land on the battlefield, " +
        "if it isn't a mana ability, this creature deals 2 damage to that player."

    triggeredAbility {
        trigger = Triggers.anOpponent.activatesAbility(
            of = (GameObjectFilter.Artifact or GameObjectFilter.Creature or GameObjectFilter.Land).onBattlefield()
        )
        effect = Effects.DealDamage(2, EffectTarget.PlayerRef(Player.TriggeringPlayer))
        description = "Whenever an opponent activates an ability of an artifact, creature, or land on the battlefield, " +
            "if it isn't a mana ability, this creature deals 2 damage to that player."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "135"
        artist = "Shreya Shetty"
        flavorText = "\"Fight through it.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/9/8936bf9d-c973-4bce-b5c2-2a01b7953638.jpg?1783936487"
        ruling(
            "2017-04-18",
            "Harsh Mentor's ability doesn't trigger when an opponent activates an ability of a card in hand " +
                "(such as cycling) or a card in a graveyard (such as embalm), even if that causes a card to be put onto the battlefield."
        )
    }
}
