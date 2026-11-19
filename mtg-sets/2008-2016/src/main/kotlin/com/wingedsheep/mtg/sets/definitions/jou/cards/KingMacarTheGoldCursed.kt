package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * King Macar, the Gold-Cursed — Journey into Nyx #74
 * {2}{B}{B} · Legendary Creature — Human Noble · 2/3
 *
 * Inspired — Whenever King Macar becomes untapped, you may exile target creature. If you do,
 * create a Gold token.
 *
 * "You may … If you do" is one optional sequence: [Effects.May] over exile `then` [Effects.CreateGold],
 * decided on resolution. If the target is illegal by then the ability doesn't resolve, so no Gold.
 * The token goes to the ability's controller, not the exiled creature's controller (per ruling).
 */
val KingMacarTheGoldCursed = card("King Macar, the Gold-Cursed") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Human Noble"
    power = 2
    toughness = 3
    oracleText = "Inspired — Whenever King Macar becomes untapped, you may exile target creature. " +
        "If you do, create a Gold token. (It's an artifact with \"Sacrifice this token: Add one mana of any color.\")"

    triggeredAbility {
        trigger = Triggers.self.becomesUntapped()
        val t = target(TargetFilter.Creature)
        effect = Effects.May(Effects.Exile(t) then Effects.CreateGold())
        description = "Inspired — Whenever King Macar becomes untapped, you may exile target creature. " +
            "If you do, create a Gold token."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "74"
        artist = "Greg Staples"
        imageUri = "https://cards.scryfall.io/normal/front/f/f/ff5987ab-570a-426c-ae4a-a270fac6b346.jpg?1783939434"

        ruling("2014-04-26", "The player who controlled King Macar when its ability triggered will control the artifact token, not the player who controlled the creature that was exiled.")
        ruling("2014-04-26", "Inspired abilities trigger no matter how the creature becomes untapped: by the turn-based action at the beginning of the untap step or by a spell or ability.")
        ruling("2014-04-26", "If an inspired ability triggers during your untap step, the ability will be put on the stack at the beginning of your upkeep. If the ability creates one or more token creatures, those creatures won't be able to attack that turn (unless they gain haste).")
        ruling("2014-04-26", "Inspired abilities don't trigger when the creature enters the battlefield.")
        ruling("2014-04-26", "If the inspired ability includes an optional cost, you decide whether to pay that cost as the ability resolves. You can do this even if the creature leaves the battlefield in response to the ability.")
    }
}
