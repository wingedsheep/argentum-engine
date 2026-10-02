package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.TriggeredAbility

/**
 * Indebted Spirit — Modern Horizons 3 #31
 * {W} · Enchantment Creature — Spirit · 1/1
 *
 * Afterlife 1 has no SDK keyword, so it is composed: "When this permanent is put into a graveyard
 * from the battlefield, create a 1/1 white and black Spirit creature token with flying." The
 * self-dies trigger also fires when the permanent is a bestowed Aura (an enchantment, not a
 * creature) going to the graveyard. Bestowed, the same afterlife trigger is granted to the
 * enchanted creature (default attached-creature scope), alongside +1/+1.
 */
val IndebtedSpirit = card("Indebted Spirit") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Enchantment Creature — Spirit"
    power = 1
    toughness = 1
    oracleText = "Bestow {2}{W} (If you cast this card for its bestow cost, it's an Aura spell with enchant creature. It becomes a creature again if it's not attached.)\n" +
        "Afterlife 1 (When this permanent is put into a graveyard from the battlefield, create a 1/1 white and black Spirit creature token with flying.)\n" +
        "Enchanted creature gets +1/+1 and has afterlife 1."

    val afterlifeTrigger = Triggers.self.dies()
    val afterlifeToken = Effects.CreateToken(
        power = 1,
        toughness = 1,
        colors = setOf(Color.WHITE, Color.BLACK),
        creatureTypes = setOf("Spirit"),
        keywords = setOf(Keyword.FLYING),
        imageUri = "https://cards.scryfall.io/normal/front/e/f/ef77c8eb-aa24-46d5-8036-6651c7602383.jpg?1783911111",
    )
    val afterlifeText = "Afterlife 1 (When this permanent is put into a graveyard from the battlefield, " +
        "create a 1/1 white and black Spirit creature token with flying.)"

    keywordAbility(KeywordAbility.bestow("{2}{W}"))

    triggeredAbility {
        trigger = afterlifeTrigger
        effect = afterlifeToken
        description = afterlifeText
    }

    staticAbility { ability = ModifyStats(1, 1) }
    staticAbility {
        ability = GrantTriggeredAbility(
            ability = TriggeredAbility.create(
                trigger = afterlifeTrigger,
                effect = afterlifeToken,
                descriptionOverride = afterlifeText,
            )
        )
    }

    metadata {
        ruling("2024-06-07", "If Indebted Spirit is put into a graveyard from the battlefield while it's an Aura, its afterlife ability will still trigger.")
        ruling("2024-06-07", "If a bestowed Indebted Spirit and the creature it's attached to are put into a graveyard from the battlefield at the same time, each of their afterlife abilities will trigger.")
        ruling("2024-06-07", "Unlike other Aura spells, an Aura spell with bestow isn't countered if its target is illegal as it begins to resolve. Rather, the effect making it an Aura spell ends, it loses enchant creature, it returns to being an enchantment creature spell, and it resolves and enters the battlefield as an enchantment creature.")
        ruling("2024-06-07", "Unlike other Auras, an Aura with bestow isn't put into its owner's graveyard if it becomes unattached. Rather, the effect making it an Aura ends, it loses enchant creature, and it remains on the battlefield as an enchantment creature.")
        ruling("2024-06-07", "If a permanent with bestow enters the battlefield by any method other than being cast, it will be an enchantment creature. You can't choose to pay the bestow cost and have it become an Aura.")
        rarity = Rarity.UNCOMMON
        collectorNumber = "31"
        artist = "L.A. Draws"
        imageUri = "https://cards.scryfall.io/normal/front/b/f/bfdbef00-bc1b-4dd6-aef5-aeb5ce454344.jpg?1783911300"
    }
}
