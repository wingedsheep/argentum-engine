package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeywordToOwnSpells
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Heartflame Duelist // Heartflame Slash
 * {1}{W}
 * Creature — Human Knight
 * 3/1
 * Instant and sorcery spells you control have lifelink.
 *
 * Adventure: Heartflame Slash — {2}{R}, Instant — Adventure
 * Heartflame Slash deals 3 damage to any target.
 *
 * The lifelink clause is [GrantKeywordToOwnSpells] over [GameObjectFilter.InstantOrSorcery] — the
 * keyword is projected onto the *spell on the stack*, and the noncombat-damage path consults the
 * spell source's granted keywords, so a burn spell you control gains you that much life. It grants
 * to spells you control regardless of who owns them, matching "spells you control".
 *
 * Note the two faces are differently coloured ({1}{W} creature, {2}{R} Adventure), so the card's
 * colour identity is RW even though the front face is mono-white.
 *
 * (CR 715: Adventure cards. Casting the Adventure exiles the card on resolution and lets the
 * caster cast it as the creature spell while it remains in exile.)
 */
val HeartflameDuelist = card("Heartflame Duelist") {
    manaCost = "{1}{W}"
    colorIdentity = "RW"
    typeLine = "Creature — Human Knight"
    oracleText = "Instant and sorcery spells you control have lifelink."
    power = 3
    toughness = 1

    staticAbility {
        ability = GrantKeywordToOwnSpells(
            keyword = Keyword.LIFELINK,
            spellFilter = GameObjectFilter.InstantOrSorcery
        )
    }

    adventure("Heartflame Slash") {
        manaCost = "{2}{R}"
        typeLine = "Instant — Adventure"
        oracleText = "Heartflame Slash deals 3 damage to any target. " +
            "(Then exile this card. You may cast the creature later from exile.)"
        spell {
            val t = target("target", Targets.Any)
            effect = Effects.DealDamage(3, t)
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "228"
        artist = "Justyna Dura"
        flavorText = "\"The fire of Embereth will never be doused!\""
        imageUri = "https://cards.scryfall.io/normal/front/8/1/811b283f-22f3-47b1-a802-11dc8c25d0ee.jpg?1783915065"
    }
}
