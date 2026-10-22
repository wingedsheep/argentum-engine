package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.daybound
import com.wingedsheep.sdk.dsl.nightbound
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AttachToPlayerAsTransforms
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Curse of Leeches // Leeching Lurker (Innistrad: Midnight Hunt)
 * {2}{B}
 * Enchantment — Aura Curse // Creature — Leech Horror 4/4
 *
 * Front — Curse of Leeches: Enchant player. "As this permanent transforms into Curse of Leeches,
 *          attach it to a player." "At the beginning of enchanted player's upkeep, they lose 1 life
 *          and you gain 1 life." Daybound.
 * Back  — Leeching Lurker: Lifelink. Nightbound.
 *
 * The as-transforms attach is [AttachToPlayerAsTransforms] on the Aura face: the controller picks the
 * player while the card turns over (CR 712.20), so it is never an unattached Aura. Turning into
 * Leeching Lurker leaves a creature attached to a player, which the CR 704.5p state-based action
 * unattaches (the card's own ruling).
 */

private const val UPKEEP_TEXT =
    "At the beginning of enchanted player's upkeep, they lose 1 life and you gain 1 life."

private val CurseOfLeechesFront = card("Curse of Leeches") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Aura Curse"
    oracleText = "Enchant player\n" +
        "As this permanent transforms into Curse of Leeches, attach it to a player.\n" +
        "$UPKEEP_TEXT\n" +
        "Daybound (If a player casts no spells during their own turn, it becomes night next turn.)"

    auraTarget = Targets.Player

    replacementEffect(AttachToPlayerAsTransforms())

    triggeredAbility {
        trigger = Triggers.player(Player.EnchantedPlayer).beginningOf(Step.UPKEEP)
        effect = Effects.LoseLife(1, EffectTarget.PlayerRef(Player.EnchantedPlayer)) then
            Effects.GainLife(1)
        description = UPKEEP_TEXT
    }
    daybound()

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "94"
        artist = "Uriah Voth"
        imageUri = "https://cards.scryfall.io/normal/front/0/a/0a3c8532-92f5-41db-92b4-a871aa05e0c7.jpg?1783925627"
    }
}

private val LeechingLurker = card("Leeching Lurker") {
    manaCost = ""
    colorIdentity = "B"
    colorIndicator = "B" // Transformed back face, no mana cost (CR 204).
    typeLine = "Creature — Leech Horror"
    power = 4
    toughness = 4
    oracleText = "Lifelink\n" +
        "Nightbound (If a player casts at least two spells during their own turn, it becomes day next turn.)"

    keywords(Keyword.LIFELINK)
    nightbound()

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "94"
        artist = "Uriah Voth"
        imageUri = "https://cards.scryfall.io/normal/back/0/a/0a3c8532-92f5-41db-92b4-a871aa05e0c7.jpg?1783925627"
    }
}

val CurseOfLeeches: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = CurseOfLeechesFront,
    backFace = LeechingLurker,
)
