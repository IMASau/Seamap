( function ( blocks, element, blockEditor ) {
    const el = element.createElement;
    const RichText = blockEditor.RichText;
    const useBlockProps = blockEditor.useBlockProps;

    blocks.registerBlockType( 'gutenberg-examples/example-01-basic', {
        apiVersion: 2,
        title: 'Example: Basic',
        icon: 'universal-access-alt',
        category: 'layout',

        attributes: {
            content: {
                type: 'string',
                source: 'html',
                selector: 'p',
            },
        },
        example: {
            attributes: {
                content: 'Hello World',
            },
        },
        edit: function (props) {
            console.log("EDIT");
            console.log("props");
            console.log(props);
            const blockProps = useBlockProps();
            console.log("blockProps");
            console.log(blockProps);
            const content = props.attributes.content;
            function onChangeContent( newContent ) {
                console.log("onChangeContent");
                console.log(newContent);
                props.setAttributes( { content: newContent.target.value } );
            }

            return el(
                'input',
                Object.assign(
                    blockProps,
                    {
                        type: 'text',
                        onChange: onChangeContent,
                        value: content,
                    }
                )
            );
        },
        save: function (props) {
            const blockProps = blockEditor.useBlockProps.save();

            const mapId = `${blockProps.id}-map`

            return el(
                'div',
                blockProps,
                [
                    el('h1', {}, props.attributes.content),
                    el('div', {id: mapId, class: 'map'}),
                    el('link', {rel: 'stylesheet', href: 'https://unpkg.com/leaflet@1.8.0/dist/leaflet.css'}),
                    el('script', {src: 'https://unpkg.com/leaflet@1.8.0/dist/leaflet.js'}),
                    el(
                        'script', {},
                        `
                            const map = L.map('${mapId}', {maxZoom: 19});

                            map.fitBounds([
                                [-43.0250008605, 115.44975869100477],
                                [-16.92549066149018, 153.40000306800002]]);                    

                            L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                                maxZoom: 19,
                                attribution: '© OpenStreetMap'
                            }).addTo(map);

                            // const content = '${props.attributes.content}';

                            // const now = Date.now();
                            // console.warn(\`\${now.toLocaleString()} \${content}\`);

                            // const p = document.getElementById('unique-id');
                            // console.warn(p);
                            
                            // const xhttp = new XMLHttpRequest();
                            // xhttp.onload = function() {
                            //     p.innerHTML = this.responseText + '\\n' + content;
                            // }
                            // xhttp.open('GET', 'http://localhost:8888/wp-json/wp/v2/story_map?acf_format=standard', true);
                            // xhttp.send();
                        `
                    )
                ]
            );
        },
    } );
} )( window.wp.blocks, window.wp.element, window.wp.blockEditor );
