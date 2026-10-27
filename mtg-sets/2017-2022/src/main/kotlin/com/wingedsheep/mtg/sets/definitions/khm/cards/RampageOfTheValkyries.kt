package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Rampage of the Valkyries — Kaldheim #393
 * {3}{W}{B} · Enchantment
 * When this enchantment enters, create a 4/4 white Angel creature token with flying and vigilance.
 * Whenever an Angel you control dies, each other player sacrifices a creature of their choice.
 *
 * The dies trigger watches any Angel you control (ruling), not only the token. "Each other player" is
 * [Player.EachOtherPlayer], the same idiom as Grave Pact — in Two-Headed Giant it includes your teammate.
 * Kaldheim's token set only has the 4/4 Angel Warrior, so the plain Angel token uses the most recent
 * 4/4 flying, vigilance Angel token art (shared with Valkyrie Harbinger).
 */
val RampageOfTheValkyries = card("Rampage of the Valkyries") {
    manaCost = "{3}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, create a 4/4 white Angel creature token with flying and vigilance.\n" +
        "Whenever an Angel you control dies, each other player sacrifices a creature of their choice."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            power = 4,
            toughness = 4,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Angel"),
            keywords = setOf(Keyword.FLYING, Keyword.VIGILANCE),
            imageUri = "https://cards.scryfall.io/normal/front/9/8/9864812a-b3b1-4e12-8737-0795cdff994c.jpg?1783902808",
        )
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Permanent.withSubtype("Angel").youControl()).dies()
        effect = Effects.Sacrifice(GameObjectFilter.Creature, 1, EffectTarget.PlayerRef(Player.EachOtherPlayer))
        description = "Whenever an Angel you control dies, each other player sacrifices a creature of their choice."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "393"
        artist = "Billy Christian"
        flavorText = "\"This is a foul place. Cleanse it.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/e/1e928b7c-ca30-4e2d-adf7-965f111c8bf1.jpg?1783928115"
        ruling("2021-02-05", "The last ability of Rampage of the Valkyries will trigger whenever any Angel you control dies, not just the one created by the first ability.")
        ruling("2021-02-05", "As the last ability resolves, if it's not your turn, the first player to choose a creature they control is the player whose turn it is. If it is your turn, the next player in turn order chooses first. In either case, each other player in turn order then chooses a creature they control, knowing the choices made before their choice. Then all chosen creatures are sacrificed at the same time.")
    }
}
