package com.wingedsheep.engine.handlers.effects.token

/**
 * Engine-wide fallback token art keyed by creature type — the last layer of token art resolution,
 * below an explicit `imageUri` on the effect and below the minting set's own
 * [com.wingedsheep.engine.registry.TokenArtRegistry] entry.
 *
 * These are deliberately *generic*: one reasonable Scryfall printing per creature type, not the
 * printing any particular card points at. A token whose art has to be exactly right belongs in its
 * set's `MtgSet.tokenArt`. What this table guarantees is that a created token is never left
 * without an image — without it the client falls back to guessing from the token's name via
 * `api.scryfall.com/cards/named`, which silently returns an arbitrary set's art.
 *
 * `TokenArtCoverageTest` holds that guarantee across the whole card corpus: every creature type
 * any registered card can mint has an entry here.
 *
 * URLs are Scryfall **`art_crop`**, matching the art box in the client's generated token frame.
 */
object TokenArt {

    /**
     * Generic art for a token with these creature types, or null when none of them is known.
     *
     * Multi-type tokens ("Zombie Army", "Zombie Druid") take the first type that has art, so a
     * token composed of a known type and a novel one still renders.
     */
    fun forCreatureTypes(creatureTypes: Collection<String>): String? =
        creatureTypes.firstNotNullOfOrNull { IMAGES[it] }

    val IMAGES: Map<String, String> = mapOf(
            "Ally" to "https://cards.scryfall.io/art_crop/front/0/1/01439983-8394-4a0c-9e9e-92a3f1927fe3.jpg?1783904746",
            "Angel" to "https://cards.scryfall.io/art_crop/front/c/7/c7f3264a-7b4a-4fef-af73-d4241742a4e8.jpg?1561758046",
            "Ape" to "https://cards.scryfall.io/art_crop/front/8/3/8343e00c-5fc6-46a0-a238-3759338dced4.jpg?1562542388",
            "Assassin" to "https://cards.scryfall.io/art_crop/front/8/9/89eb9f92-d189-4438-b6fe-cb253055d63e.jpg?1562539812",
            "Bat" to "https://cards.scryfall.io/art_crop/front/1/0/100c0127-49dd-4a78-9c88-1881e7923674.jpg?1721425184",
            "Bear" to "https://cards.scryfall.io/art_crop/front/0/a/0a21bc37-6f21-4dda-a313-a0d75696f7fc.jpg?1561756625",
            "Beast" to "https://cards.scryfall.io/art_crop/front/c/e/ce45e037-5efb-4735-afee-12d7dc3127d1.jpg?1561758106",
            "Bird" to "https://cards.scryfall.io/art_crop/front/2/6/26e0e196-36e6-4d7a-a76b-1c2a18270267.jpg?1561756807",
            "Boar" to "https://cards.scryfall.io/art_crop/front/a/f/afb796a0-4eb0-4fc5-bf84-92a71bec4466.jpg?1675455830",
            // Camarid (FEM — Homarid Spawning Bed). Wizards never printed a Camarid token card, so
            // Scryfall has no art for one at all; the closest real image is Fallen Empires' own
            // Homarid, the crustacean that breeds them.
            "Camarid" to "https://cards.scryfall.io/art_crop/front/d/6/d6ffeab4-83b1-4414-ae72-e59a2354ea15.jpg?1783947913",
            "Cat" to "https://cards.scryfall.io/art_crop/front/5/2/5252ab51-43e8-4b24-9830-de0ad9b9d3dc.jpg?1562164858",
            "Centaur" to "https://cards.scryfall.io/art_crop/front/d/c/dcd41697-6fe6-423c-a04a-035a3d4f8fd2.jpg?1675456051",
            "Citizen" to "https://cards.scryfall.io/art_crop/front/1/6/165164e7-5693-4d65-b789-8ed8a222365b.jpg?1547509191",
            "Cleric" to "https://cards.scryfall.io/art_crop/front/9/a/9af74e26-69d7-4683-9a71-9b420c03d75f.jpg?1561757657",
            "Construct" to "https://cards.scryfall.io/art_crop/front/3/8/3877d2d1-61f2-4295-b0fc-827965eaaefc.jpg?1712317494",
            "Crab" to "https://cards.scryfall.io/art_crop/front/7/e/7ef7f37a-b7f5-45a1-8f2b-7097089ca2e5.jpg?1641306174",
            "Demon" to "https://cards.scryfall.io/art_crop/front/0/8/084c3292-c3ec-431e-b162-309b5cf6309e.jpg?1561756614",
            "Devil" to "https://cards.scryfall.io/art_crop/front/d/7/d7a88466-7f44-4535-a8d5-097ed747454d.jpg?1726237228",
            "Dinosaur" to "https://cards.scryfall.io/art_crop/front/c/7/c75c01ea-427d-4401-b5b0-166e87c4585e.jpg?1712316260",
            "Djinn" to "https://cards.scryfall.io/art_crop/front/f/2/f2e8077e-4400-4923-afe6-6ff5a51b5e91.jpg?1561758421",
            "Dog" to "https://cards.scryfall.io/art_crop/front/8/a/8aaf03be-294a-4539-954c-79853f4351a9.jpg?1706216959",
            "Dragon" to "https://cards.scryfall.io/art_crop/front/5/6/56c0fa29-fbcb-41fe-94ab-a39b1154c99b.jpg?1561757167",
            "Drake" to "https://cards.scryfall.io/art_crop/front/6/5/65844e72-aa84-483f-b07e-9e34e798aaec.jpg?1625511529",
            "Dryad" to "https://cards.scryfall.io/art_crop/front/7/4/74de70f2-93b6-4fc5-8c4d-464f880d3c54.jpg?1730838400",
            "Dwarf" to "https://cards.scryfall.io/art_crop/front/1/4/142bbbe1-4446-4d80-b82d-0df8ef17eac1.jpg?1615686398",
            "Elemental" to "https://cards.scryfall.io/art_crop/front/1/c/1c791044-be86-47d8-8c1b-299f6ab254e6.jpg?1562636696",
            "Elephant" to "https://cards.scryfall.io/art_crop/front/2/4/243bcfa9-0310-4d68-9864-df46069906fa.jpg?1743176747",
            "Elf" to "https://cards.scryfall.io/art_crop/front/2/7/27b171ac-b2ef-4a80-92d1-6d9e71f3e3ca.jpg?1562636717",
            "Faerie" to "https://cards.scryfall.io/art_crop/front/1/6/1666cae8-8750-4091-8e45-259e76268db9.jpg?1561756717",
            "Fish" to "https://cards.scryfall.io/art_crop/front/1/f/1f3cea7c-d092-410d-8c32-af0f4c8bc878.jpg?1562541939",
            "Fox" to "https://cards.scryfall.io/art_crop/front/d/3/d31d906e-e5c2-48ad-b7b0-f740ea4447ff.jpg?1717190096",
            "Frog" to "https://cards.scryfall.io/art_crop/front/e/3/e3c84944-23b8-40d7-9b25-c746b08b4dc4.jpg?1748704089",
            "Giant" to "https://cards.scryfall.io/art_crop/front/0/6/0661166a-7d6c-4cba-8190-8d3eedf7f58c.jpg?1561756604",
            "Goat" to "https://cards.scryfall.io/art_crop/front/9/0/90f67615-8a09-4ab9-9927-899a15e72c03.jpg?1561757576",
            "Goblin" to "https://cards.scryfall.io/art_crop/front/f/b/fbd728ed-d5dc-44b3-9159-6c86cab3be0c.jpg?1761614931",
            "Golem" to "https://cards.scryfall.io/art_crop/front/4/0/406e2960-f560-48bb-b4a6-4bd35889a8f8.jpg?1712318018",
            "Gremlin" to "https://cards.scryfall.io/art_crop/front/d/9/d948b503-890a-49d5-a3cf-cb6e604851b8.jpg?1783909269",
            "Griffin" to "https://cards.scryfall.io/art_crop/front/9/6/96c36884-df5e-435d-9473-65da550535fb.jpg?1561757626",
            "Harpy" to "https://cards.scryfall.io/art_crop/front/2/6/26b24bbe-f5bc-44d3-b716-6612d39b07bc.jpg?1562636770",
            "Hellion" to "https://cards.scryfall.io/art_crop/front/a/c/acabc3ce-a3c1-4c6c-8022-9a96ffba59c6.jpg?1561757810",
            "Hippo" to "https://cards.scryfall.io/art_crop/front/1/a/1aea5e0b-dc4e-4055-9e13-1dfbc25a2f00.jpg?1562844782",
            "Homunculus" to "https://cards.scryfall.io/art_crop/front/e/2/e2020f53-d012-4d26-be13-87ed0f196c53.jpg?1783940883",
            "Horror" to "https://cards.scryfall.io/art_crop/front/5/a/5a4c5954-da2b-498a-9e86-eb4d6dc95cb8.jpg?1561757207",
            "Horse" to "https://cards.scryfall.io/art_crop/front/b/c/bc944579-b6d8-40f7-8c46-146513960d61.jpg?1761614913",
            "Human" to "https://cards.scryfall.io/art_crop/front/b/d/bd5cd362-34f9-445f-85e1-9f6694f0f90a.jpg?1561757948",
            "Hydra" to "https://cards.scryfall.io/art_crop/front/5/7/57cad0da-ac47-4324-9810-cf2ae94fd5e0.jpg?1762360009",
            "Illusion" to "https://cards.scryfall.io/art_crop/front/5/d/5dcbf662-7263-414a-b64b-ccf9aab20faa.jpg?1562636807",
            "Imp" to "https://cards.scryfall.io/art_crop/front/4/7/47a1385b-2be2-49a8-8400-186cd5525dad.jpg?1706217208",
            "Insect" to "https://cards.scryfall.io/art_crop/front/a/a/aa47df37-f246-4f80-a944-008cdf347dad.jpg?1561757793",
            "Jellyfish" to "https://cards.scryfall.io/art_crop/front/1/9/19ac0a35-fae7-49f9-ae96-4406df992dc9.jpg?1562639696",
            "Kavu" to "https://cards.scryfall.io/art_crop/front/e/5/e50f89a6-26bd-4144-8c4a-ee9f293bd9c4.jpg?1783921131",
            "Knight" to "https://cards.scryfall.io/art_crop/front/b/f/bf9acfe1-de7a-48fe-aed3-28a72db6d1c0.jpg?1561757975",
            "Kobold" to "https://cards.scryfall.io/art_crop/front/d/f/dfc03591-1114-4e36-a397-0bb3db8a153c.jpg?1562702392",
            "Kraken" to "https://cards.scryfall.io/art_crop/front/c/b/cb727dec-dd82-4072-b2ec-a4e31b58752f.jpg?1682206995",
            "Lizard" to "https://cards.scryfall.io/art_crop/front/8/3/83790fd3-f371-494c-97a2-3aa469a399f1.jpg?1561757482",
            "Mercenary" to "https://cards.scryfall.io/art_crop/front/5/f/5f04607f-eed2-462e-897f-82e41e5f7049.jpg?1712316319",
            "Merfolk" to "https://cards.scryfall.io/art_crop/front/f/b/fb1b292b-2da6-4601-9f93-5eb273ce3a50.jpg?1562636943",
            "Minotaur" to "https://cards.scryfall.io/art_crop/front/6/2/62a8926c-94d7-4399-ad38-f235bbfd1a7e.jpg?1561757290",
            "Monk" to "https://cards.scryfall.io/art_crop/front/1/e/1e498e42-f55c-4afa-b2e2-02345f91cdb5.jpg?1561756357",
            "Ninja" to "https://cards.scryfall.io/art_crop/front/a/e/aeec04b1-475c-4e55-b72f-327ea5258146.jpg?1732302748",
            "Octopus" to "https://cards.scryfall.io/art_crop/front/1/9/19ac0a35-fae7-49f9-ae96-4406df992dc9.jpg?1562639696",
            "Ogre" to "https://cards.scryfall.io/art_crop/front/3/c/3ca43425-d007-4181-9182-18dc01ad7e90.jpg?1674337914",
            "Ooze" to "https://cards.scryfall.io/art_crop/front/c/2/c2fc764f-d5fe-452a-8474-5ad380048faf.jpg?1771590231",
            "Otter" to "https://cards.scryfall.io/art_crop/front/e/6/e6b2c465-c446-4dee-9101-763105dcf813.jpg?1783909764",
            "Pegasus" to "https://cards.scryfall.io/art_crop/front/b/c/bc944579-b6d8-40f7-8c46-146513960d61.jpg?1761614913",
            "Pest" to "https://cards.scryfall.io/art_crop/front/d/0/d0ddbe3e-4a66-494d-9304-7471232549bf.jpg?1783927190",
            "Pirate" to "https://cards.scryfall.io/art_crop/front/4/6/46bf5e2b-869f-480e-ac37-1bdb40a92f8c.jpg?1665776397",
            "Rabbit" to "https://cards.scryfall.io/art_crop/front/8/1/81de52ef-7515-4958-abea-fb8ebdcef93c.jpg?1721431122",
            "Raccoon" to "https://cards.scryfall.io/art_crop/front/a/f/af257e8b-9bed-4c6b-9510-11fe950fa80e.jpg?1783909752",
            "Rat" to "https://cards.scryfall.io/art_crop/front/1/a/1a85fe9d-ef18-46c4-88b0-cf2e222e30e4.jpg?1562279130",
            "Rhino" to "https://cards.scryfall.io/art_crop/front/1/3/1331008a-ae86-4640-b823-a73be766ac16.jpg",
            "Robot" to "https://cards.scryfall.io/art_crop/front/0/8/08497fc5-1c0e-4c3c-a356-bf4b34bd4c45.jpg?1783904001",
            "Rogue" to "https://cards.scryfall.io/art_crop/front/f/4/f44d5271-5d10-46b2-9ba2-5788d99de2e6.jpg?1562636888",
            "Samurai" to "https://cards.scryfall.io/art_crop/front/7/0/70750c90-3856-4d6d-923b-2ab91b1d7049.jpg?1675957554",
            "Saproling" to "https://cards.scryfall.io/art_crop/front/0/b/0bf3d41e-cd0a-46bd-8b89-8855906ea6b5.jpg?1702501311",
            "Satyr" to "https://cards.scryfall.io/art_crop/front/9/0/903e30f3-580e-4a14-989b-ae0632363407.jpg?1581902165",
            "Scarecrow" to "https://cards.scryfall.io/art_crop/front/9/a/9ae02771-2917-4d6e-9608-1a592389439a.jpg?1767955222",
            "Serpent" to "https://cards.scryfall.io/art_crop/front/5/4/54a1c6a9-3531-4432-9157-e4400dbc89fd.jpg",
            "Shapeshifter" to "https://cards.scryfall.io/art_crop/front/1/a/1a7d89ca-8611-4bda-b5c8-0350ce091102.jpg",
            "Shark" to "https://cards.scryfall.io/art_crop/front/f/9/f9424ef2-d271-4929-83e0-12775420bac3.jpg?1721427369",
            "Skeleton" to "https://cards.scryfall.io/art_crop/front/6/8/6894192e-782b-49ef-b9fc-28b76e2268ab.jpg?1562636766",
            "Sliver" to "https://cards.scryfall.io/art_crop/front/b/f/bf7501ee-783d-49c6-b381-1db056360b40.jpg?1752946465",
            "Snake" to "https://cards.scryfall.io/art_crop/front/8/3/83a6a142-f065-4a74-9a73-8105be29bc94.jpg?1562636831",
            "Soldier" to "https://cards.scryfall.io/art_crop/front/b/1/b159b57d-bc52-4cef-ac7a-e364e40c3d03.jpg?1761614919",
            "Sphinx" to "https://cards.scryfall.io/art_crop/front/f/8/f82ba894-7b10-45ae-9322-60ef85a2869d.jpg?1572892536",
            "Spider" to "https://cards.scryfall.io/art_crop/front/7/d/7df0de51-8d05-475a-832e-de8a0f60849e.jpg?1562279134",
            "Spirit" to "https://cards.scryfall.io/art_crop/front/1/4/14ef4815-3dfe-47b3-ad81-1506925280d3.jpg?1561756700",
            "Squirrel" to "https://cards.scryfall.io/art_crop/front/5/a/5a6ec62e-0e9b-4312-bfe8-cc85d76fd9e0.jpg?1721425294",
            "Thopter" to "https://cards.scryfall.io/art_crop/front/7/8/78e52380-13a1-44fe-b762-e71261cac3d0.jpg?1738355254",
            "Treefolk" to "https://cards.scryfall.io/art_crop/front/2/a/2a3f0d52-34cd-4095-bfa0-bbf9562a8146.jpg",
            "Troll" to "https://cards.scryfall.io/art_crop/front/8/8/8869a8cc-d196-417f-bba5-5ed31bae6a18.jpg?1615686760",
            "Vampire" to "https://cards.scryfall.io/art_crop/front/9/6/969eff58-d91e-49e2-a1e1-8f32b4598810.jpg?1562636856",
            "Wall" to "https://cards.scryfall.io/art_crop/front/7/f/7f31debf-0b93-44c7-99b6-be441ba4e167.jpg?1562702163",
            "Warrior" to "https://cards.scryfall.io/art_crop/front/1/2/12856d0c-240f-42c6-80dd-715ccf314645.jpg?1561756680",
            "Wizard" to "https://cards.scryfall.io/art_crop/front/7/b/7b0b95ce-4821-4955-a27c-93471240f54b.jpg?1557575896",
            "Wolf" to "https://cards.scryfall.io/art_crop/front/8/9/89b89a55-3ea2-4186-b946-06831bc16169.jpg?1736468521",
            "Wraith" to "https://cards.scryfall.io/art_crop/front/9/a/9afb58e8-f5a7-49f9-8287-77757bd3268c.jpg",
            "Wurm" to "https://cards.scryfall.io/art_crop/front/9/f/9fc04b19-e636-4868-8224-a5da75ea01c8.jpg?1561757701",
            "Zombie" to "https://cards.scryfall.io/art_crop/front/8/e/8e7b5995-ee8b-40d1-b157-8df96c02cc5b.jpg?1761614925",
    )
}
