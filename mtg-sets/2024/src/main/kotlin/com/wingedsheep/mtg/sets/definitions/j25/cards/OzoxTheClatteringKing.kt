package com.wingedsheep.mtg.sets.definitions.j25.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Ozox, the Clattering King — Foundations Jumpstart #44
 * {2}{B} · Legendary Creature — Skeleton Noble · 3/2
 *
 * Ozox can't block.
 * When Ozox dies, create Jumblebones, a legendary 2/1 black Skeleton creature token with
 * "Jumblebones can't block" and "When Jumblebones leaves the battlefield, return target card named
 * Ozox, the Clattering King from your graveyard to your hand."
 *
 * Jumblebones' trigger is a leaves-the-battlefield trigger on the token itself, so it fires on any
 * zone change off the battlefield (dying, bounce, exile), looking back in time at the token.
 *
 * Token art: Scryfall has no Jumblebones token printing (there is no `tj25` token set), so the
 * effect points at the art crop of Murders at Karlov Manor's 2/1 black Skeleton token — the same
 * body — and the client draws it inside a generated frame that carries Jumblebones' own name and
 * abilities, rather than a printed card naming a different token.
 */
val OzoxTheClatteringKing = card("Ozox, the Clattering King") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Skeleton Noble"
    power = 3
    toughness = 2
    oracleText = "Ozox can't block.\n" +
        "When Ozox dies, create Jumblebones, a legendary 2/1 black Skeleton creature token with " +
        "\"Jumblebones can't block\" and \"When Jumblebones leaves the battlefield, return target card " +
        "named Ozox, the Clattering King from your graveyard to your hand.\""

    staticAbility {
        ability = CantBlock()
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.CreateToken(
            power = 2,
            toughness = 1,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Skeleton"),
            name = "Jumblebones",
            legendary = true,
            staticAbilities = listOf(CantBlock()),
            triggeredAbilities = listOf(
                grantedTriggeredAbility {
                    trigger = Triggers.self.leaves()
                    val ozox = target(
                        TargetFilter(
                            GameObjectFilter.Any.named("Ozox, the Clattering King").ownedByYou(),
                            zone = Zone.GRAVEYARD,
                        ),
                    )
                    effect = Effects.ReturnToHand(ozox)
                    description = "When Jumblebones leaves the battlefield, return target card named " +
                        "Ozox, the Clattering King from your graveyard to your hand."
                }
            ),
            imageUri = "https://cards.scryfall.io/art_crop/front/0/2/02404852-5788-4adb-b2dc-98bf705d8d96.jpg?1783912608",
        )
        description = "When Ozox dies, create Jumblebones, a legendary 2/1 black Skeleton creature token " +
            "with \"Jumblebones can't block\" and \"When Jumblebones leaves the battlefield, return target " +
            "card named Ozox, the Clattering King from your graveyard to your hand.\""
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "44"
        artist = "Inuchiyo Meimaru"
        imageUri = "https://cards.scryfall.io/normal/front/0/2/02cf5079-9da8-470d-8eea-00d6aac68d04.jpg?1783908856"
    }
}
